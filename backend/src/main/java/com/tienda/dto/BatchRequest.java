package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequest {
    private int quantity;
    private int expirationYear;
    private int expirationMonth;
    private int expirationDay;
    private java.math.BigDecimal purchasePrice;
}
