package com.tienda.controller;

import com.tienda.dto.*;
import com.tienda.entity.StoreInventory;
import com.tienda.entity.StoreOwner;
import com.tienda.entity.User;
import com.tienda.repository.StoreOwnerRepository;
import com.tienda.repository.UserRepository;
import com.tienda.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/store")
@PreAuthorize("hasAuthority('ROLE_STORE')")
public class StoreController {

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final TicketService ticketService;
    private final SaleService saleService;
    private final StoreOwnerRepository storeOwnerRepository;
    private final UserRepository userRepository;

    public StoreController(ProductService productService,
                           OrderService orderService,
                           InventoryService inventoryService,
                           TicketService ticketService,
                           SaleService saleService,
                           StoreOwnerRepository storeOwnerRepository,
                           UserRepository userRepository) {
        this.productService = productService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.ticketService = ticketService;
        this.saleService = saleService;
        this.storeOwnerRepository = storeOwnerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> searchProducts(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.ok(productService.searchProducts(query)));
    }

    @GetMapping("/products/{id}/batches")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getProductBatches(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAvailableBatchesForProduct(id)));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<StoreInventoryDto>>> getInventory(Authentication auth) {
        Long storeId = getStoreId(auth);
        List<StoreInventory> inventory = inventoryService.getStoreInventory(storeId);
        return ResponseEntity.ok(ApiResponse.ok(inventory.stream()
                .map(this::inventoryToDto).toList()));
    }

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<ApiResponse<List<StoreInventoryDto>>> getLowStock(Authentication auth) {
        Long storeId = getStoreId(auth);
        List<StoreInventory> inventory = inventoryService.getLowStock(storeId);
        return ResponseEntity.ok(ApiResponse.ok(inventory.stream()
                .map(this::inventoryToDto).toList()));
    }

    @PutMapping("/inventory/{id}/thresholds")
    public ResponseEntity<ApiResponse<StoreInventoryDto>> updateThresholds(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody ThresholdRequest request) {
        Long storeId = getStoreId(auth);
        StoreInventory updated = inventoryService.updateThresholds(storeId, id, request.getMinStock(), request.getMaxStock());
        return ResponseEntity.ok(ApiResponse.ok("Umbrales actualizados", inventoryToDto(updated)));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            Authentication auth,
            @RequestBody List<OrderItemRequest> items) {
        Long storeId = getStoreId(auth);
        var order = orderService.createOrder(storeId, items, false);
        return ResponseEntity.ok(ApiResponse.ok("Pedido creado", toOrderDto(order)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrders(Authentication auth) {
        Long storeId = getStoreId(auth);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getStoreOrders(storeId)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable Long id) {
        var order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(toOrderDto(order)));
    }

    @PostMapping("/orders/{id}/receive")
    public ResponseEntity<ApiResponse<OrderDto>> receiveOrder(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody ReceiveOrderRequest request) {
        Long storeId = getStoreId(auth);
        var order = orderService.receiveOrder(id, storeId, request);

        if (request.isWithDiscrepancy()) {
            ticketService.createTicket(id, storeId, "ROLE_STORE", request.getDiscrepancyMessage());
        }

        return ResponseEntity.ok(ApiResponse.ok("Recepción procesada", toOrderDto(order)));
    }

    @PostMapping("/orders/{id}/dispute")
    public ResponseEntity<ApiResponse<TicketDto>> disputeOrder(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody Map body) {
        Long storeId = getStoreId(auth);
        String message = body.get("message") != null ? (String) body.get("message") : "Discrepancia reportada";
        var ticket = ticketService.createTicket(id, storeId, "ROLE_STORE", message);
        return ResponseEntity.ok(ApiResponse.ok("Ticket creado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/favorite-supplier/{supplierId}")
    public ResponseEntity<ApiResponse<Void>> setFavoriteSupplier(
            Authentication auth,
            @PathVariable Long supplierId) {
        Long storeId = getStoreId(auth);
        StoreOwner store = storeOwnerRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        store.setFavoriteSupplierId(supplierId);
        storeOwnerRepository.save(store);
        return ResponseEntity.ok(ApiResponse.ok("Proveedor favorito actualizado", null));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTickets(Authentication auth) {
        Long storeId = getStoreId(auth);
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getTicketsByStore(storeId)));
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
        Long storeId = getStoreId(auth);
        var ticket = ticketService.vote(id, storeId, "ROLE_STORE", request);
        return ResponseEntity.ok(ApiResponse.ok("Voto registrado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/cancel")
    public ResponseEntity<ApiResponse<TicketDto>> cancelTicket(
            @PathVariable Long id,
            Authentication auth) {
        Long storeId = getStoreId(auth);
        var ticket = ticketService.cancelTicket(id, storeId, "ROLE_STORE");
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelado", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/propose-price")
    public ResponseEntity<ApiResponse<TicketDto>> proposePrice(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody PriceProposalRequest request) {
        Long storeId = getStoreId(auth);
        var ticket = ticketService.proposePrice(id, storeId, "ROLE_STORE", request.getPrice());
        return ResponseEntity.ok(ApiResponse.ok("Precio propuesto", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/tickets/{id}/negotiation-response")
    public ResponseEntity<ApiResponse<TicketDto>> negotiationResponse(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody NegotiationResponseRequest request) {
        Long storeId = getStoreId(auth);
        var ticket = ticketService.respondToNegotiation(id, storeId, "ROLE_STORE", request.isAccept());
        return ResponseEntity.ok(ApiResponse.ok("Respuesta registrada", ticketService.getTicket(ticket.getId())));
    }

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<SaleDto>> createSale(
            Authentication auth,
            @RequestBody CreateSaleRequest request) {
        Long storeId = getStoreId(auth);
        SaleDto sale = saleService.createSale(storeId, request.getItems());
        return ResponseEntity.ok(ApiResponse.ok("Venta registrada", sale));
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<List<SaleDto>>> getSalesHistory(Authentication auth) {
        Long storeId = getStoreId(auth);
        return ResponseEntity.ok(ApiResponse.ok(saleService.getSalesByStore(storeId)));
    }

    private Long getStoreId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getId();
    }

    private StoreInventoryDto inventoryToDto(StoreInventory si) {
        var sp = si.getSupplierProduct();
        ProductDto productDto = ProductDto.builder()
                .id(sp != null ? sp.getId() : null)
                .code(sp != null ? sp.getCode() : null)
                .name(sp != null ? sp.getName() : null)
                .basePrice(sp != null ? sp.getBasePrice() : null)
                .minStock(si.getMinStock())
                .maxStock(si.getMaxStock())
                .supplierId(sp != null && sp.getSupplier() != null ? sp.getSupplier().getId() : null)
                .supplierName(sp != null && sp.getSupplier() != null ? sp.getSupplier().getCompanyName() : null)
                .unitId(sp != null && sp.getUnit() != null ? sp.getUnit().getId() : null)
                .unitName(sp != null && sp.getUnit() != null ? sp.getUnit().getName() : null)
                .unitAbbreviation(sp != null && sp.getUnit() != null ? sp.getUnit().getAbbreviation() : null)
                .totalStock(si.getQuantity())
                .lastUpdated(si.getLastUpdated())
                .build();

        return StoreInventoryDto.builder()
                .id(si.getId())
                .supplierProduct(productDto)
                .quantity(si.getQuantity())
                .minStock(si.getMinStock())
                .maxStock(si.getMaxStock())
                .lastUpdated(si.getLastUpdated())
                .build();
    }

    private OrderDto toOrderDto(com.tienda.entity.Order order) {
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
                .build();
    }
}
