package com.tienda.repository;

import com.tienda.entity.StoreOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreOwnerRepository extends JpaRepository<StoreOwner, Long> {
}
