package com.tienda.service;

import com.tienda.dto.*;
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
public class SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);

    private final SaleRepository saleRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SupplierProductRepository productRepository;
    private final StoreInventoryRepository inventoryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public SaleService(SaleRepository saleRepository,
                       StoreOwnerRepository storeOwnerRepository,
                       SupplierProductRepository productRepository,
                       StoreInventoryRepository inventoryRepository,
                       NotificationService notificationService,
                       AuditService auditService) {
        this.saleRepository = saleRepository;
        this.storeOwnerRepository = storeOwnerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public SaleDto createSale(Long storeOwnerId, List<SaleItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("La venta debe tener al menos un producto");
        }

        StoreOwner store = storeOwnerRepository.findById(storeOwnerId)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        Sale sale = Sale.builder()
                .storeOwner(store)
                .items(new ArrayList<>())
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest itemReq : items) {
            SupplierProduct product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemReq.getProductId()));

            StoreInventory inventory = inventoryRepository
                    .findByStoreOwnerIdAndSupplierProductId(storeOwnerId, itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto sin stock en inventario: " + product.getName()));

            if (inventory.getQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para " + product.getName()
                        + ". Disponible: " + inventory.getQuantity() + ", solicitado: " + itemReq.getQuantity());
            }

            inventory.setQuantity(inventory.getQuantity() - itemReq.getQuantity());
            inventoryRepository.save(inventory);

            BigDecimal unitPrice = product.getBasePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(subtotal);

            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .supplierProduct(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .build();

            sale.getItems().add(saleItem);

            if (inventory.getMinStock() > 0 && inventory.getQuantity() <= inventory.getMinStock()) {
                notificationService.notifyStoreOwner(storeOwnerId,
                        "Stock bajo tras venta: " + product.getName() + " (" + inventory.getQuantity() + " <= " + inventory.getMinStock() + ")",
                        "LOW_STOCK", product.getId());
            }
        }

        sale.setTotal(total);
        sale = saleRepository.save(sale);

        safeAudit(store.getEmail(), "SALE_CREATED", "SALE", sale.getId(), null,
                "items=" + items.size() + " total=" + total);

        return toDto(sale);
    }

    public List<SaleDto> getSalesByStore(Long storeOwnerId) {
        return saleRepository.findByStoreOwnerIdOrderBySaleDateDesc(storeOwnerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private SaleDto toDto(Sale sale) {
        List<SaleItemDto> itemDtos = sale.getItems().stream()
                .map(i -> SaleItemDto.builder()
                        .id(i.getId())
                        .productId(i.getSupplierProduct().getId())
                        .productName(i.getSupplierProduct().getName())
                        .productCode(i.getSupplierProduct().getCode())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return SaleDto.builder()
                .id(sale.getId())
                .storeName(sale.getStoreOwner().getStoreName())
                .saleDate(sale.getSaleDate())
                .total(sale.getTotal())
                .items(itemDtos)
                .itemCount(itemDtos.size())
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
