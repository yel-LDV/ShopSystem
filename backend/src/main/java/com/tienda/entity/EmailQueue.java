package com.tienda.entity;

import jakarta.persistence.*; // already has wildcard, Table included
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cola_correo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipient;

    private String subject;

    @Column(length = 5000)
    private String body;

    private int retryCount;

    private int maxRetries;

    @Enumerated(EnumType.STRING)
    private EmailStatus status;

    private LocalDateTime createdAt;

    private String lastError;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
        this.maxRetries = 3;
        this.status = EmailStatus.PENDING;
    }

    public enum EmailStatus {
        PENDING, SENT, FAILED
    }
}
