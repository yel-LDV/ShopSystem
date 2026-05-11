package com.tienda.controller;

import com.tienda.dto.*;
import com.tienda.entity.User;
import com.tienda.repository.UserRepository;
import com.tienda.service.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier")
@PreAuthorize("hasAuthority('ROLE_SUPPLIER')")
public class SupplierController {

    private final ProductService productService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final UserRepository userRepository;

    public SupplierController(ProductService productService,
                              OrderService orderService,
                              TicketService ticketService,
                              UserRepository userRepository) {
        this.productService = productService;
        this.orderService = orderService;
        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts(Authentication auth) {
        Long supplierId = getSupplierId(auth);
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductsBySupplier(supplierId)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            Authentication auth,
            @RequestBody ProductDto dto) {
        Long supplierId = getSupplierId(auth);
        var product = productService.createProduct(supplierId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Producto creado", productService.toDto(product)));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto dto) {
        var product = productService.updateProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Producto actualizado", productService.toDto(product)));
    }

    @PostMapping("/products/{id}/batches")
    public ResponseEntity<ApiResponse<Void>> addBatch(
            @PathVariable Long id,
            @RequestBody BatchRequest request) {
        productService.addBatch(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Lote agregado", null));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrders(Authentication auth) {
        Long supplierId = getSupplierId(auth);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getSupplierOrders(supplierId)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable Long id) {
        var order = orderService.getOrderById(id);
        var dto = OrderDto.builder()
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
                .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/orders/{id}/respond")
    public ResponseEntity<ApiResponse<OrderDto>> respondToOrder(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        Long supplierId = getSupplierId(auth);
        boolean accept = (boolean) body.get("accept");
        String reason = body.containsKey("reason") ? (String) body.get("reason") : null;

        var order = orderService.respondToOrder(id, supplierId, accept, reason);

        var dto = OrderDto.builder()
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
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Respuesta enviada", dto));
    }

    @PostMapping("/orders/{id}/dispute")
    public ResponseEntity<ApiResponse<TicketDto>> disputeOrder(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody Map<String, String> body) {
        Long supplierId = getSupplierId(auth);
        String message = body.getOrDefault("message", "Discrepancia reportada por proveedor");
        var ticket = ticketService.createTicket(id, supplierId, "ROLE_SUPPLIER", message);
        return ResponseEntity.ok(ApiResponse.ok("Ticket creado", ticketService.getTicket(ticket.getId())));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTickets(Authentication auth) {
        Long supplierId = getSupplierId(auth);
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicketsBySupplier(supplierId)));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<TicketDto>> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicket(id)));
    }

    @PostMapping("/tickets/{id}/vote")
    public ResponseEntity<ApiResponse<TicketDto>> vote(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody VoteRequest request) {
        Long supplierId = getSupplierId(auth);
        var ticket = ticketService.vote(id, supplierId, "ROLE_SUPPLIER", request);
        return ResponseEntity.ok(ApiResponse.ok("Voto registrado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/cancel")
    public ResponseEntity<ApiResponse<TicketDto>> cancelTicket(
            @PathVariable Long id,
            Authentication auth) {
        Long supplierId = getSupplierId(auth);
        var ticket = ticketService.cancelTicket(id, supplierId, "ROLE_SUPPLIER");
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/propose-price")
    public ResponseEntity<ApiResponse<TicketDto>> proposePrice(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody PriceProposalRequest request) {
        Long supplierId = getSupplierId(auth);
        var ticket = ticketService.proposePrice(id, supplierId, "ROLE_SUPPLIER", request.getPrice());
        return ResponseEntity.ok(ApiResponse.ok("Precio propuesto", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/negotiation-response")
    public ResponseEntity<ApiResponse<TicketDto>> negotiationResponse(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody NegotiationResponseRequest request) {
        Long supplierId = getSupplierId(auth);
        var ticket = ticketService.respondToNegotiation(id, supplierId, "ROLE_SUPPLIER", request.isAccept());
        return ResponseEntity.ok(ApiResponse.ok("Respuesta registrada", ticketService.getTicket(ticket.getId())));
    }

    private Long getSupplierId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getId();
    }
}
