package com.tienda.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proveedor")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Supplier extends User {

    private String companyName;
    private String contactPhone;
    private String emergencyEmail;
    private String address;
}
