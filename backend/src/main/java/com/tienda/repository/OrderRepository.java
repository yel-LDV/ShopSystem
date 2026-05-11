package com.tienda.repository;

import com.tienda.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStoreOwnerIdOrderByCreatedAtDesc(Long storeOwnerId);

    List<Order> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    List<Order> findByStoreOwnerIdAndStatus(Long storeOwnerId, Order.OrderStatus status);

    List<Order> findBySupplierIdAndStatus(Long supplierId, Order.OrderStatus status);
}
