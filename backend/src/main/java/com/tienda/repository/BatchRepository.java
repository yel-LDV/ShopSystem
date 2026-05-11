package com.tienda.repository;

import com.tienda.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Query("SELECT b FROM Batch b WHERE b.supplierProduct.id = :productId AND b.expirationDate > CURRENT_DATE AND (b.quantity - b.reservedQuantity) > 0 ORDER BY b.expirationDate ASC")
    List<Batch> findAvailableBatchesByProductId(Long productId);

    List<Batch> findBySupplierProductId(Long productId);
}
