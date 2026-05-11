package com.tienda.repository;

import com.tienda.entity.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {

    List<StoreInventory> findByStoreOwnerId(Long storeOwnerId);

    Optional<StoreInventory> findByStoreOwnerIdAndSupplierProductId(Long storeOwnerId, Long productId);

    @Query("SELECT si FROM StoreInventory si WHERE si.storeOwner.id = :storeOwnerId AND si.quantity <= si.minStock")
    List<StoreInventory> findLowStockByStoreOwnerId(Long storeOwnerId);
}
