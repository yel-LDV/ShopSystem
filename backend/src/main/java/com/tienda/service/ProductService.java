package com.tienda.service;

import com.tienda.dto.BatchRequest;
import com.tienda.dto.ProductDto;
import com.tienda.entity.*;
import com.tienda.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final SupplierProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AuditService auditService;

    public ProductService(SupplierProductRepository productRepository,
                          BatchRepository batchRepository,
                          SupplierRepository supplierRepository,
                          UnitOfMeasureRepository unitRepository,
                          PriceHistoryRepository priceHistoryRepository,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.auditService = auditService;
    }

    public List<ProductDto> getProductsBySupplier(Long supplierId) {
        return productRepository.findBySupplierId(supplierId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return productRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return productRepository.searchByTerm(query).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierProduct createProduct(Long supplierId, ProductDto dto) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        if (productRepository.existsByNameAndSupplierId(dto.getName(), supplierId)) {
            throw new RuntimeException("Ya existe un producto con ese nombre para este proveedor");
        }

        UnitOfMeasure unit;
        if (dto.getUnitId() != null) {
            unit = unitRepository.findById(dto.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unidad de medida no encontrada"));
        } else {
            unit = unitRepository.findByName("Pieza")
                    .orElseGet(() -> {
                        UnitOfMeasure u = new UnitOfMeasure();
                        u.setName("Pieza");
                        u.setAbbreviation("pza");
                        return unitRepository.save(u);
                    });
        }

        SupplierProduct product = SupplierProduct.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .creationDate(LocalDate.now())
                .basePrice(dto.getBasePrice())
                .minStock(dto.getMinStock())
                .maxStock(dto.getMaxStock())
                .supplier(supplier)
                .unit(unit)
                .build();

        product = productRepository.save(product);

        safeAudit(supplier.getEmail(), "PRODUCT_CREATED", "PRODUCT", product.getId(), null, dto.getName());

        return product;
    }

    @Transactional
    public SupplierProduct updateProduct(Long productId, ProductDto dto) {
        SupplierProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (dto.getBasePrice() != null && dto.getBasePrice().compareTo(product.getBasePrice()) != 0) {
            PriceHistory history = PriceHistory.builder()
                    .supplierProduct(product)
                    .oldPrice(product.getBasePrice())
                    .newPrice(dto.getBasePrice())
                    .build();
            priceHistoryRepository.save(history);

            safeAudit(product.getSupplier().getEmail(), "PRICE_CHANGED", "PRODUCT", productId,
                    product.getBasePrice().toString(), dto.getBasePrice().toString());
        }

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getBasePrice() != null) product.setBasePrice(dto.getBasePrice());
        if (dto.getMinStock() > 0) product.setMinStock(dto.getMinStock());
        if (dto.getMaxStock() > 0) product.setMaxStock(dto.getMaxStock());

        product = productRepository.save(product);

        safeAudit(product.getSupplier().getEmail(), "PRODUCT_UPDATED", "PRODUCT", productId, null, dto.getName() != null ? dto.getName() : product.getName());

        return product;
    }

    @Transactional
    public Batch addBatch(Long productId, BatchRequest request) {
        SupplierProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        LocalDate expirationDate = LocalDate.of(
                request.getExpirationYear(),
                request.getExpirationMonth(),
                request.getExpirationDay()
        );

        Batch batch = Batch.builder()
                .quantity(request.getQuantity())
                .expirationDate(expirationDate)
                .purchasePrice(request.getPurchasePrice() != null ? request.getPurchasePrice() : product.getBasePrice())
                .supplierProduct(product)
                .build();

        batch = batchRepository.save(batch);

        safeAudit(product.getSupplier().getEmail(), "BATCH_ADDED", "PRODUCT", productId, null, "qty=" + request.getQuantity());

        return batch;
    }

    public int getAvailableStock(Long productId) {
        return batchRepository.findAvailableBatchesByProductId(productId).stream()
                .mapToInt(b -> b.getQuantity() - b.getReservedQuantity())
                .sum();
    }

    @Transactional
    public void deleteProduct(Long productId) {
        SupplierProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        batchRepository.deleteAll(batchRepository.findBySupplierProductId(productId));
        priceHistoryRepository.deleteAll(priceHistoryRepository.findBySupplierProductId(productId));
        productRepository.delete(product);

        safeAudit("admin", "PRODUCT_DELETED", "PRODUCT", productId, product.getName(), null);
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto toDto(SupplierProduct sp) {
        int totalStock = getAvailableStock(sp.getId());
        return ProductDto.builder()
                .id(sp.getId())
                .code(sp.getCode())
                .name(sp.getName())
                .creationDate(sp.getCreationDate())
                .expirationDate(sp.getExpirationDate())
                .basePrice(sp.getBasePrice())
                .minStock(sp.getMinStock())
                .maxStock(sp.getMaxStock())
                .supplierId(sp.getSupplier().getId())
                .supplierName(sp.getSupplier().getCompanyName())
                .unitId(sp.getUnit() != null ? sp.getUnit().getId() : null)
                .unitName(sp.getUnit() != null ? sp.getUnit().getName() : null)
                .unitAbbreviation(sp.getUnit() != null ? sp.getUnit().getAbbreviation() : null)
                .totalStock(totalStock)
                .build();
    }

    private void safeAudit(String username, String action, String entityType, Long entityId, String oldValue, String newValue) {
        try {
            auditService.log(username, action, entityType, entityId, oldValue, newValue);
        } catch (Exception e) {
            log.error("Error al auditar {} {}: {}", action, entityType, e.getMessage());
        }
    }
}
