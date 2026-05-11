package com.tienda.repository;

import com.tienda.entity.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    List<SupplierProduct> findBySupplierId(Long supplierId);

    @Query("SELECT sp FROM SupplierProduct sp WHERE LOWER(sp.name) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(sp.code) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<SupplierProduct> searchByTerm(String term);

    List<SupplierProduct> findByNameContainingIgnoreCase(String name);

    boolean existsByNameAndSupplierId(String name, Long supplierId);
}
