package com.tienda.service;

import com.tienda.dto.*;
import com.tienda.entity.*;
import com.tienda.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SupplierRepository supplierRepository;
    private final AdminUserRepository adminUserRepository;

    @Value("${app.ticket.voting-duration-minutes:5}")
    private int votingDurationMinutes;

    public TicketService(TicketRepository ticketRepository,
                         MessageRepository messageRepository,
                         OrderRepository orderRepository,
                         OrderService orderService,
                         InventoryService inventoryService,
                         NotificationService notificationService,
                         AuditService auditService,
                         StoreOwnerRepository storeOwnerRepository,
                         SupplierRepository supplierRepository,
                         AdminUserRepository adminUserRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.storeOwnerRepository = storeOwnerRepository;
        this.supplierRepository = supplierRepository;
        this.adminUserRepository = adminUserRepository;
    }

    @Transactional
    public Ticket createTicket(Long orderId, Long userId, String userRole, String initialMessage) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Ticket ticket = Ticket.builder()
                .storeOwner(order.getStoreOwner())
                .supplier(order.getSupplier())
                .order(order)
                .votingEndDate(LocalDateTime.now().plusMinutes(votingDurationMinutes))
                .build();

        ticket = ticketRepository.save(ticket);

        if (initialMessage != null && !initialMessage.isBlank()) {
            Message message = Message.builder()
                    .ticket(ticket)
                    .senderId(userId)
                    .senderRole(userRole)
                    .content(initialMessage)
                    .build();
            messageRepository.save(message);
        }

        String creatorLabel = "ROLE_STORE".equals(userRole) ? "tienda" : "proveedor";
        safeAudit("user-" + userId, "TICKET_CREATED", "TICKET", ticket.getId(), null,
                "order=" + orderId + " by " + creatorLabel);

        Long notifyTarget = "ROLE_STORE".equals(userRole)
                ? order.getSupplier().getId()
                : order.getStoreOwner().getId();

        String notifyType = "ROLE_STORE".equals(userRole) ? "ROLE_SUPPLIER" : "ROLE_STORE";

        if ("ROLE_STORE".equals(notifyType)) {
            notificationService.notifyStoreOwner(notifyTarget,
                    "Ticket #" + ticket.getId() + " abierto para pedido #" + orderId,
                    "TICKET_CREATED", ticket.getId());
        } else {
            notificationService.notifySupplier(notifyTarget,
                    "Ticket #" + ticket.getId() + " abierto para pedido #" + orderId,
                    "TICKET_CREATED", ticket.getId());
        }

        return ticket;
    }

    @Transactional
    public Ticket cancelTicket(Long ticketId, Long userId, String userRole) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED) {
            throw new RuntimeException("El ticket ya esta resuelto");
        }

        Order order = ticket.getOrder();
        for (OrderItem item : order.getItems()) {
            inventoryService.releaseReservations(item.getSupplierProduct().getId(), item.getQuantity());
        }

        ticket.setFinalResolution(Ticket.ResolutionType.CANCEL);
        ticket.setStatus(Ticket.TicketStatus.RESOLVED);

        order.setStatus(Order.OrderStatus.REJECTED_BY_SUPPLIER);
        orderRepository.save(order);

        ticket = ticketRepository.save(ticket);

        safeAudit("user-" + userId, "TICKET_CANCELLED", "TICKET", ticketId, ticket.getStatus().name(), "CANCEL by " + userRole);

        Long storeId = ticket.getStoreOwner().getId();
        Long supplierId = ticket.getSupplier().getId();
        notificationService.notifyStoreOwner(storeId, "Ticket #" + ticketId + " cancelado", "TICKET_CANCELLED", ticketId);
        notificationService.notifySupplier(supplierId, "Ticket #" + ticketId + " cancelado", "TICKET_CANCELLED", ticketId);

        return ticket;
    }

    @Transactional
    public Message addMessage(Long ticketId, Long senderId, String senderRole, String content) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Message message = Message.builder()
                .ticket(ticket)
                .senderId(senderId)
                .senderRole(senderRole)
                .content(content)
                .build();

        message = messageRepository.save(message);
        safeAudit("user-" + senderId, "TICKET_MESSAGE", "TICKET", ticketId, null, senderRole);
        return message;
    }

    @Transactional
    public Ticket vote(Long ticketId, Long userId, String userRole, VoteRequest voteRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED) {
            throw new RuntimeException("El ticket ya esta resuelto");
        }

        Ticket.ResolutionType vote = Ticket.ResolutionType.valueOf(voteRequest.getResolution().toUpperCase());

        if ("ROLE_STORE".equals(userRole)) {
            ticket.setStoreOwnerVote(vote);
        } else if ("ROLE_SUPPLIER".equals(userRole)) {
            ticket.setSupplierVote(vote);
        } else {
            throw new RuntimeException("Rol no permitido para votar");
        }

        ticket.setStatus(Ticket.TicketStatus.VOTING);
        ticket = ticketRepository.save(ticket);

        safeAudit("user-" + userId, "TICKET_VOTED", "TICKET", ticketId, null, vote.name() + " by " + userRole);

        Ticket.ResolutionType consensus = checkConsensus(ticket);
        if (consensus != null) {
            applyResolution(ticket, consensus);
            ticketRepository.save(ticket);
            safeAudit("system", "TICKET_CONSENSUS", "TICKET", ticket.getId(), "VOTING", consensus.name());
            notifyParties(ticket, "Ticket #" + ticketId + " resuelto por consenso: " + consensus.name());
        } else if (ticket.getStoreOwnerVote() != null && ticket.getSupplierVote() != null) {
            notifyAdmin("Ticket #" + ticketId + " sin consenso. Se requiere decision del admin.",
                    "TICKET_DISAGREEMENT", ticketId);
        }

        return ticket;
    }

    @Transactional
    public Ticket adminVote(Long ticketId, Long adminId, VoteRequest voteRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED) {
            throw new RuntimeException("El ticket ya esta resuelto");
        }

        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        Ticket.ResolutionType vote = Ticket.ResolutionType.valueOf(voteRequest.getResolution().toUpperCase());
        ticket.setAdminVote(vote);
        ticket.setAdmin(admin);
        applyResolution(ticket, vote);
        ticket = ticketRepository.save(ticket);

        safeAudit("admin-" + adminId, "TICKET_ADMIN_RESOLVED", "TICKET", ticketId, "VOTING", vote.name());
        notifyParties(ticket, "Ticket #" + ticketId + " resuelto por admin: " + vote.name());

        return ticket;
    }

    @Transactional
    public Ticket proposePrice(Long ticketId, Long userId, String userRole, BigDecimal proposedPrice) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED) {
            throw new RuntimeException("El ticket ya esta resuelto");
        }

        if (ticket.getNegotiationStatus() == Ticket.NegotiationStatus.PROPOSED) {
            throw new RuntimeException("Ya hay una negociacion de precio pendiente");
        }

        if (ticket.getNegotiationStatus() == Ticket.NegotiationStatus.REJECTED) {
            throw new RuntimeException("La negociacion ya fue rechazada, el admin debe decidir");
        }

        ticket.setProposedPrice(proposedPrice);
        ticket.setPriceProposedBy(userRole);
        ticket.setNegotiationStatus(Ticket.NegotiationStatus.PROPOSED);
        ticket.setStatus(Ticket.TicketStatus.NEGOTIATING);
        ticket = ticketRepository.save(ticket);

        safeAudit("user-" + userId, "TICKET_PRICE_PROPOSED", "TICKET", ticketId, null,
                "price=" + proposedPrice + " by " + userRole);

        if ("ROLE_STORE".equals(userRole)) {
            notificationService.notifySupplier(ticket.getSupplier().getId(),
                    "Nueva propuesta de precio $" + proposedPrice + " para ticket #" + ticketId,
                    "TICKET_NEGOTIATION", ticketId);
        } else {
            notificationService.notifyStoreOwner(ticket.getStoreOwner().getId(),
                    "Nueva propuesta de precio $" + proposedPrice + " para ticket #" + ticketId,
                    "TICKET_NEGOTIATION", ticketId);
        }

        return ticket;
    }

    @Transactional
    public Ticket respondToNegotiation(Long ticketId, Long userId, String userRole, boolean accept) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (ticket.getNegotiationStatus() != Ticket.NegotiationStatus.PROPOSED) {
            throw new RuntimeException("No hay una negociacion de precio pendiente");
        }

        if (ticket.getPriceProposedBy().equals(userRole)) {
            throw new RuntimeException("No puedes responder a tu propia propuesta");
        }

        if (accept) {
            ticket.setNegotiationStatus(Ticket.NegotiationStatus.ACCEPTED);
            ticket.setFinalResolution(Ticket.ResolutionType.ACCEPT);
            ticket.setStatus(Ticket.TicketStatus.RESOLVED);
            applyResolutionOnOrder(ticket, Ticket.ResolutionType.ACCEPT);
            ticketRepository.save(ticket);

            safeAudit("user-" + userId, "TICKET_NEGOTIATION_ACCEPTED", "TICKET", ticketId, null,
                    "price=" + ticket.getProposedPrice());
            notifyParties(ticket, "Ticket #" + ticketId + " resuelto: precio negociado $" + ticket.getProposedPrice());
        } else {
            ticket.setNegotiationStatus(Ticket.NegotiationStatus.REJECTED);
            ticket.setStatus(Ticket.TicketStatus.VOTING);
            ticketRepository.save(ticket);

            safeAudit("user-" + userId, "TICKET_NEGOTIATION_REJECTED", "TICKET", ticketId, null, "REJECTED");
            notifyAdmin("Ticket #" + ticketId + ": negociacion rechazada. Se requiere decision del admin.",
                    "TICKET_DISAGREEMENT", ticketId);
        }

        return ticket;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void resolveExpiredTickets() {
        List<Ticket> expired = ticketRepository
                .findByStatusAndVotingEndDateBefore(Ticket.TicketStatus.VOTING, LocalDateTime.now());

        for (Ticket ticket : expired) {
            if (ticket.getStoreOwnerVote() == null || ticket.getSupplierVote() == null) {
                notificationService.notifyAdmin(
                        "Ticket #" + ticket.getId() + " expiro. Una o ambas partes no votaron. Se requiere decision del admin.",
                        "TICKET_EXPIRED", ticket.getId());
            }
        }

        List<Ticket> expiredNegotiating = ticketRepository
                .findByStatusAndVotingEndDateBefore(Ticket.TicketStatus.NEGOTIATING, LocalDateTime.now());

        for (Ticket ticket : expiredNegotiating) {
            notificationService.notifyAdmin(
                    "Ticket #" + ticket.getId() + " expiro durante negociacion. Se requiere decision del admin.",
                    "TICKET_EXPIRED", ticket.getId());
        }
    }

    @Transactional
    public Ticket adminResolve(Long ticketId, VoteRequest voteRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Ticket.ResolutionType resolution = Ticket.ResolutionType.valueOf(voteRequest.getResolution().toUpperCase());
        applyResolution(ticket, resolution);

        safeAudit("admin", "TICKET_RESOLVED", "TICKET", ticketId, ticket.getStatus().name(), resolution.name());
        notifyParties(ticket, "Ticket #" + ticketId + " resuelto por admin: " + resolution.name());

        return ticketRepository.save(ticket);
    }

    private Ticket.ResolutionType checkConsensus(Ticket ticket) {
        if (ticket.getStoreOwnerVote() != null
                && ticket.getSupplierVote() != null
                && ticket.getStoreOwnerVote() == ticket.getSupplierVote()) {
            return ticket.getStoreOwnerVote();
        }
        return null;
    }

    private void applyResolution(Ticket ticket, Ticket.ResolutionType resolution) {
        ticket.setFinalResolution(resolution);
        ticket.setStatus(Ticket.TicketStatus.RESOLVED);
        applyResolutionOnOrder(ticket, resolution);
    }

    private void applyResolutionOnOrder(Ticket ticket, Ticket.ResolutionType resolution) {
        Order order = ticket.getOrder();

        switch (resolution) {
            case CANCEL:
                for (OrderItem item : order.getItems()) {
                    inventoryService.releaseReservations(item.getSupplierProduct().getId(), item.getQuantity());
                }
                order.setStatus(Order.OrderStatus.REJECTED_BY_SUPPLIER);
                break;

            case ACCEPT:
            case DISCOUNT:
                applyNegotiatedPriceIfPresent(ticket, order);

                for (OrderItem item : order.getItems()) {
                    inventoryService.deductFromBatches(item.getSupplierProduct().getId(), item.getQuantity());
                    inventoryService.addOrUpdateInventory(
                            order.getStoreOwner().getId(),
                            item.getSupplierProduct().getId(),
                            item.getQuantity()
                    );
                }
                order.setStatus(Order.OrderStatus.RECEIVED);
                break;
        }

        orderRepository.save(order);
    }

    private void applyNegotiatedPriceIfPresent(Ticket ticket, Order order) {
        BigDecimal proposedPrice = ticket.getProposedPrice();
        if (proposedPrice == null || proposedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal originalTotal = order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (originalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            BigDecimal proportion = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .divide(originalTotal, 6, RoundingMode.HALF_UP);
            BigDecimal newUnitPrice = proposedPrice.multiply(proportion)
                    .divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
            item.setUnitPrice(newUnitPrice);
        }

        BigDecimal diff = originalTotal.subtract(proposedPrice);
        int discountPct = diff.divide(originalTotal, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).intValue();
        ticket.setDiscountPercentage(Math.max(0, discountPct));
    }

    private void notifyParties(Ticket ticket, String message) {
        notificationService.notifyStoreOwner(ticket.getStoreOwner().getId(), message, "TICKET_RESOLVED", ticket.getId());
        notificationService.notifySupplier(ticket.getSupplier().getId(), message, "TICKET_RESOLVED", ticket.getId());
    }

    private void notifyAdmin(String message, String type, Long referenceId) {
        notificationService.notifyAdmin(message, type, referenceId);
    }

    public TicketDto getTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        return toDto(ticket);
    }

    public List<TicketDto> getTicketsByStore(Long storeOwnerId) {
        return ticketRepository.findByStoreOwnerIdOrderByCreatedAtDesc(storeOwnerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TicketDto> getTicketsBySupplier(Long supplierId) {
        return ticketRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TicketDto> getTicketsByStatus(List<Ticket.TicketStatus> statuses) {
        return ticketRepository.findByStatusInOrderByCreatedAtDesc(statuses).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<TicketDto> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private TicketDto toDto(Ticket ticket) {
        List<MessageDto> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(m -> MessageDto.builder()
                        .id(m.getId())
                        .ticketId(m.getTicket().getId())
                        .senderId(m.getSenderId())
                        .senderRole(m.getSenderRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return TicketDto.builder()
                .id(ticket.getId())
                .orderId(ticket.getOrder() != null ? ticket.getOrder().getId() : null)
                .storeName(ticket.getStoreOwner() != null ? ticket.getStoreOwner().getStoreName() : null)
                .supplierName(ticket.getSupplier() != null ? ticket.getSupplier().getCompanyName() : null)
                .status(ticket.getStatus().name())
                .createdAt(ticket.getCreatedAt())
                .votingEndDate(ticket.getVotingEndDate())
                .finalResolution(ticket.getFinalResolution())
                .discountPercentage(ticket.getDiscountPercentage())
                .storeOwnerVote(ticket.getStoreOwnerVote())
                .supplierVote(ticket.getSupplierVote())
                .adminVote(ticket.getAdminVote())
                .proposedPrice(ticket.getProposedPrice())
                .priceProposedBy(ticket.getPriceProposedBy())
                .negotiationStatus(ticket.getNegotiationStatus())
                .messages(messages)
                .build();
    }

    private void safeAudit(String username, String action, String entityType, Long entityId, String oldValue, String newValue) {
        try {
            auditService.log(username, action, entityType, entityId, oldValue, newValue);
        } catch (Exception e) {
            log.error("Error al auditar {} {}: {}", action, entityType, e.getMessage());
        }
    }
}
