package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
    private Long id;
    private int quantity;
    private int availableQuantity;
    private LocalDate expirationDate;
    private BigDecimal purchasePrice;
    private LocalDateTime createdAt;
    private Long productId;
}
