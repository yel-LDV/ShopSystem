package com.tienda.service;

import com.tienda.dto.OrderItemRequest;
import com.tienda.entity.StoreInventory;
import com.tienda.repository.StoreInventoryRepository;
import com.tienda.repository.StoreOwnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AutoReorderService {

    private final StoreOwnerRepository storeOwnerRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final OrderService orderService;

    public AutoReorderService(StoreOwnerRepository storeOwnerRepository,
                              StoreInventoryRepository storeInventoryRepository,
                              OrderService orderService) {
        this.storeOwnerRepository = storeOwnerRepository;
        this.storeInventoryRepository = storeInventoryRepository;
        this.orderService = orderService;
    }

    @Scheduled(cron = "${app.reorder.cron}")
    public void checkAndReorder() {
        log.info("Ejecutando reorden automatico...");

        List<StoreInventory> allInventories = storeInventoryRepository.findAll();

        for (StoreInventory item : allInventories) {
            try {
                var product = item.getSupplierProduct();
                if (product == null) continue;

                int effectiveMinStock = item.getMinStock() > 0 ? item.getMinStock() : product.getMinStock();
                int effectiveMaxStock = item.getMaxStock() > 0 ? item.getMaxStock() : product.getMaxStock();

                if (item.getQuantity() <= effectiveMinStock) {
                    int desired = effectiveMaxStock - item.getQuantity();
                    if (desired > 0) {
                        List<OrderItemRequest> items = List.of(
                                OrderItemRequest.builder()
                                        .productId(product.getId())
                                        .quantity(desired)
                                        .build()
                        );
                        orderService.createOrder(item.getStoreOwner().getId(), items, true);
                        log.info("Pedido automatico creado para producto {} (tienda {})",
                                product.getName(), item.getStoreOwner().getStoreName());
                    }
                }
            } catch (Exception e) {
                log.error("Error en reorden automatico para inventario {}: {}", item.getId(), e.getMessage());
            }
        }

        log.info("Reorden automatico completado.");
    }
}
