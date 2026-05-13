package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String code;
    private Long id;
    private String name;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private BigDecimal basePrice;
    private int minStock;
    private int maxStock;
    private Long supplierId;
    private String supplierName;
    private Long unitId;
    private String unitName;
    private String unitAbbreviation;
    private int totalStock;
    private LocalDateTime lastUpdated;
    private List<BatchDto> batches;
}
