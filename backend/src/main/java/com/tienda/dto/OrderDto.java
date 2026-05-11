package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String storeName;
    private String supplierName;
    private Long supplierId;
    private String status;
    private boolean isAutomatic;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private String rejectionReason;
    private int itemCount;
    private BigDecimal total;
}
