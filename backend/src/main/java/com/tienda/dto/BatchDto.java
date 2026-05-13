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
public class BatchDto {
    private int quantity;
    private int expirationYear;
    private int expirationMonth;
    private int expirationDay;
    private BigDecimal purchasePrice;
}
