package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
