package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreInventoryDto {
    private Long id;
    private ProductDto supplierProduct;
    private int quantity;
    private int minStock;
    private int maxStock;
    private LocalDateTime lastUpdated;
}
