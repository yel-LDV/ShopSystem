package com.tienda.service;

import com.tienda.entity.*;
import com.tienda.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final StoreInventoryRepository inventoryRepository;
    private final BatchRepository batchRepository;
    private final NotificationService notificationService;

    public InventoryService(StoreInventoryRepository inventoryRepository,
                            BatchRepository batchRepository,
                            NotificationService notificationService) {
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.notificationService = notificationService;
    }

    public List<StoreInventory> getStoreInventory(Long storeOwnerId) {
        return inventoryRepository.findByStoreOwnerId(storeOwnerId);
    }

    public List<StoreInventory> getLowStock(Long storeOwnerId) {
        return inventoryRepository.findLowStockByStoreOwnerId(storeOwnerId);
    }

    @Transactional
    public StoreInventory addOrUpdateInventory(Long storeOwnerId, Long productId, int quantity) {
        StoreInventory inventory = inventoryRepository
                .findByStoreOwnerIdAndSupplierProductId(storeOwnerId, productId)
                .orElseGet(() -> StoreInventory.builder()
                        .storeOwner(new StoreOwner())
                        .supplierProduct(new SupplierProduct())
                        .quantity(0)
                        .minStock(0)
                        .maxStock(0)
                        .build());

        if (inventory.getId() == null) {
            StoreOwner store = new StoreOwner();
            store.setId(storeOwnerId);
            inventory.setStoreOwner(store);

            SupplierProduct product = new SupplierProduct();
            product.setId(productId);
            inventory.setSupplierProduct(product);
        }

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory = inventoryRepository.save(inventory);

        checkAndNotifyLowStock(inventory);

        return inventory;
    }

    @Transactional
    public StoreInventory updateThresholds(Long storeOwnerId, Long inventoryId, int minStock, int maxStock) {
        StoreInventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

        if (!inventory.getStoreOwner().getId().equals(storeOwnerId)) {
            throw new RuntimeException("No tienes permiso para modificar este inventario");
        }

        inventory.setMinStock(minStock);
        inventory.setMaxStock(maxStock);
        inventory = inventoryRepository.save(inventory);

        checkAndNotifyLowStock(inventory);

        return inventory;
    }

    private void checkAndNotifyLowStock(StoreInventory inventory) {
        if (inventory.getMinStock() > 0 && inventory.getQuantity() <= inventory.getMinStock()) {
            String productName = inventory.getSupplierProduct() != null
                    ? inventory.getSupplierProduct().getName() : "Producto";

            notificationService.notifyStoreOwner(
                    inventory.getStoreOwner().getId(),
                    "Stock bajo: " + productName + " (" + inventory.getQuantity() + " <= " + inventory.getMinStock() + ")",
                    "LOW_STOCK",
                    inventory.getSupplierProduct() != null ? inventory.getSupplierProduct().getId() : null
            );
        }
    }

    @Transactional
    public void deductFromBatches(Long productId, int quantityToDeduct) {
        List<Batch> availableBatches = batchRepository.findAvailableBatchesByProductId(productId);
        int remaining = quantityToDeduct;

        for (Batch batch : availableBatches) {
            if (remaining <= 0) break;
            int available = batch.getQuantity() - batch.getReservedQuantity();
            int toDeduct = Math.min(remaining, available);
            batch.setQuantity(batch.getQuantity() - toDeduct);
            batch.setReservedQuantity(batch.getReservedQuantity() - toDeduct);
            batchRepository.save(batch);
            remaining -= toDeduct;
        }

        if (remaining > 0) {
            throw new RuntimeException("Stock insuficiente para deducir " + quantityToDeduct);
        }
    }

    @Transactional
    public void reserveBatches(Long productId, int quantityToReserve) {
        List<Batch> availableBatches = batchRepository.findAvailableBatchesByProductId(productId);
        int remaining = quantityToReserve;

        for (Batch batch : availableBatches) {
            if (remaining <= 0) break;
            int available = batch.getQuantity() - batch.getReservedQuantity();
            int toReserve = Math.min(remaining, available);
            batch.setReservedQuantity(batch.getReservedQuantity() + toReserve);
            batchRepository.save(batch);
            remaining -= toReserve;
        }

        if (remaining > 0) {
            throw new RuntimeException("Stock insuficiente para reservar " + quantityToReserve);
        }
    }

    @Transactional
    public void releaseReservations(Long productId, int quantityToRelease) {
        List<Batch> batches = batchRepository.findBySupplierProductId(productId);
        int remaining = quantityToRelease;

        for (Batch batch : batches) {
            if (remaining <= 0) break;
            int toRelease = Math.min(remaining, batch.getReservedQuantity());
            batch.setReservedQuantity(batch.getReservedQuantity() - toRelease);
            batchRepository.save(batch);
            remaining -= toRelease;
        }
    }

    @Transactional
    public void reserveBatch(Long batchId, int quantity) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + batchId));
        int available = batch.getQuantity() - batch.getReservedQuantity();
        if (quantity > available) {
            throw new RuntimeException("Stock insuficiente en lote #" + batchId
                    + " (disponible: " + available + ", solicitado: " + quantity + ")");
        }
        batch.setReservedQuantity(batch.getReservedQuantity() + quantity);
        batchRepository.save(batch);
    }

    @Transactional
    public void deductBatch(Long batchId, int quantity) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + batchId));
        int available = batch.getQuantity() - batch.getReservedQuantity();
        if (quantity > available) {
            throw new RuntimeException("Stock insuficiente en lote #" + batchId
                    + " (disponible: " + available + ", solicitado: " + quantity + ")");
        }
        batch.setQuantity(batch.getQuantity() - quantity);
        batch.setReservedQuantity(batch.getReservedQuantity() - quantity);
        batchRepository.save(batch);
    }
}
