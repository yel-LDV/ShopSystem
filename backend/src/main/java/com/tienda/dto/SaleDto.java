package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleDto {
    private Long id;
    private String storeName;
    private LocalDateTime saleDate;
    private BigDecimal total;
    private List<SaleItemDto> items;
    private int itemCount;
}
