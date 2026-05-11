package com.tienda.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StoreOwner extends User {

    private String storeName;
    private String address;
    private Long favoriteSupplierId;
}
