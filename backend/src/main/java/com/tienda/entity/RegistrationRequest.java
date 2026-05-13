package com.tienda.entity;

import jakarta.persistence.*; // already has wildcard, Table included
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_registro")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String fullName;

    private String role;

    private String storeName;
    private String storeAddress;

    private String companyName;
    private String contactPhone;
    private String emergencyEmail;
    private String address;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
}
