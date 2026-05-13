package com.tienda.service;

import com.tienda.dto.OrderDto;
import com.tienda.dto.OrderItemRequest;
import com.tienda.dto.ReceiveOrderRequest;
import com.tienda.entity.*;
import com.tienda.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final TicketRepository ticketRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        StoreOwnerRepository storeOwnerRepository,
                        SupplierRepository supplierRepository,
                        SupplierProductRepository productRepository,
                        BatchRepository batchRepository,
                        TicketRepository ticketRepository,
                        InventoryService inventoryService,
                        NotificationService notificationService,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.storeOwnerRepository = storeOwnerRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.ticketRepository = ticketRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Order createOrder(Long storeOwnerId, List<OrderItemRequest> items, boolean isAutomatic) {
        StoreOwner store = storeOwnerRepository.findById(storeOwnerId)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        if (items.isEmpty()) {
            throw new RuntimeException("El pedido debe tener al menos un producto");
        }

        Supplier selectedSupplier = selectSupplier(store, items.get(0).getProductId());

        Order order = Order.builder()
                .storeOwner(store)
                .supplier(selectedSupplier)
                .isAutomatic(isAutomatic)
                .items(new ArrayList<>())
                .build();

        order = orderRepository.save(order);

        for (OrderItemRequest itemReq : items) {
            SupplierProduct product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemReq.getProductId()));

            if (itemReq.getBatchId() != null) {
                Batch batch = batchRepository.findById(itemReq.getBatchId())
                        .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + itemReq.getBatchId()));
                if (!batch.getSupplierProduct().getId().equals(product.getId())) {
                    throw new RuntimeException("El lote no pertenece al producto seleccionado");
                }
                int available = batch.getQuantity() - batch.getReservedQuantity();
                if (itemReq.getQuantity() > available) {
                    throw new RuntimeException("Stock insuficiente en lote #" + batch.getId()
                            + " (disponible: " + available + ", solicitado: " + itemReq.getQuantity() + ")");
                }
            }

            BigDecimal unitPrice = product.getBasePrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .supplierProduct(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .batchId(itemReq.getBatchId())
                    .build();

            order.getItems().add(orderItem);
        }

        order = orderRepository.save(order);

        safeAudit(store.getEmail(), "ORDER_CREATED", "ORDER", order.getId(), null,
                "store=" + store.getStoreName() + " supplier=" + selectedSupplier.getCompanyName() + " items=" + items.size());

        notificationService.notifySupplier(selectedSupplier.getId(),
                "Nuevo pedido #" + order.getId() + " de " + store.getStoreName(),
                "ORDER_CREATED", order.getId());

        return order;
    }

    private Supplier selectSupplier(StoreOwner store, Long productId) {
        if (store.getFavoriteSupplierId() != null) {
            SupplierProduct product = productRepository.findById(productId).orElse(null);
            if (product != null && product.getSupplier().getId().equals(store.getFavoriteSupplierId())) {
                return product.getSupplier();
            }
        }

        SupplierProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("No hay proveedores para este producto"));

        return product.getSupplier();
    }

    @Transactional
    public Order respondToOrder(Long orderId, Long supplierId, boolean accept, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (!order.getSupplier().getId().equals(supplierId)) {
            throw new RuntimeException("Este pedido no te pertenece");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("El pedido ya fue respondido");
        }

        String supplierEmail = order.getSupplier().getEmail();

        if (accept) {
            for (OrderItem item : order.getItems()) {
                if (item.getBatchId() != null) {
                    inventoryService.reserveBatch(item.getBatchId(), item.getQuantity());
                } else {
                    inventoryService.reserveBatches(item.getSupplierProduct().getId(), item.getQuantity());
                }
            }
            order.setStatus(Order.OrderStatus.ACCEPTED_BY_SUPPLIER);
            order.setRespondedAt(java.time.LocalDateTime.now());

            safeAudit(supplierEmail, "ORDER_ACCEPTED", "ORDER", order.getId(), "PENDING", "ACCEPTED_BY_SUPPLIER");

            notificationService.notifyStoreOwner(order.getStoreOwner().getId(),
                    "Pedido #" + order.getId() + " aceptado por el proveedor",
                    "ORDER_ACCEPTED", order.getId());
        } else {
            order.setStatus(Order.OrderStatus.REJECTED_BY_SUPPLIER);
            order.setRejectionReason(reason);
            order.setRespondedAt(java.time.LocalDateTime.now());

            safeAudit(supplierEmail, "ORDER_REJECTED", "ORDER", order.getId(), "PENDING", "REJECTED: " + reason);

            notificationService.notifyStoreOwner(order.getStoreOwner().getId(),
                    "Pedido #" + order.getId() + " rechazado: " + reason,
                    "ORDER_REJECTED", order.getId());
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order receiveOrder(Long orderId, Long storeOwnerId, ReceiveOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (!order.getStoreOwner().getId().equals(storeOwnerId)) {
            throw new RuntimeException("Este pedido no te pertenece");
        }

        if (order.getStatus() != Order.OrderStatus.ACCEPTED_BY_SUPPLIER) {
            throw new RuntimeException("El pedido no esta en estado para recibir");
        }

        String storeEmail = order.getStoreOwner().getEmail();

        if (request.isWithDiscrepancy()) {
            order.setStatus(Order.OrderStatus.DISPUTED);

            safeAudit(storeEmail, "ORDER_DISPUTED", "ORDER", order.getId(), "ACCEPTED_BY_SUPPLIER", "DISPUTED: " + request.getDiscrepancyMessage());
        } else {
            for (OrderItem item : order.getItems()) {
                if (item.getBatchId() != null) {
                    inventoryService.deductBatch(item.getBatchId(), item.getQuantity());
                } else {
                    inventoryService.deductFromBatches(item.getSupplierProduct().getId(), item.getQuantity());
                }
                inventoryService.addOrUpdateInventory(storeOwnerId, item.getSupplierProduct().getId(), item.getQuantity());
            }
            order.setStatus(Order.OrderStatus.RECEIVED);

            safeAudit(storeEmail, "ORDER_RECEIVED", "ORDER", order.getId(), "ACCEPTED_BY_SUPPLIER", "RECEIVED");
        }

        order.setReceivedAt(java.time.LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<OrderDto> getStoreOrders(Long storeOwnerId) {
        return orderRepository.findByStoreOwnerIdOrderByCreatedAtDesc(storeOwnerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getSupplierOrders(Long supplierId) {
        return orderRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    private OrderDto toDto(Order order) {
        Long ticketId = ticketRepository.findByOrderId(order.getId())
                .map(Ticket::getId)
                .orElse(null);

        return OrderDto.builder()
                .id(order.getId())
                .storeName(order.getStoreOwner().getStoreName())
                .supplierName(order.getSupplier().getCompanyName())
                .supplierId(order.getSupplier().getId())
                .status(order.getStatus().name())
                .isAutomatic(order.isAutomatic())
                .createdAt(order.getCreatedAt())
                .respondedAt(order.getRespondedAt())
                .rejectionReason(order.getRejectionReason())
                .itemCount(order.getItems().size())
                .total(order.getItems().stream()
                        .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .ticketId(ticketId)
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
