package com.tienda.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_usuario")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AdminUser extends User {
}
