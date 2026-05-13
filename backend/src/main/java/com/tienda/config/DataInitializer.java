package com.tienda.config;

import com.tienda.entity.User;
import com.tienda.entity.AdminUser;
import com.tienda.entity.Supplier;
import com.tienda.entity.StoreOwner;
import com.tienda.entity.StoreInventory;
import com.tienda.entity.UnitOfMeasure;
import com.tienda.entity.SupplierProduct;
import com.tienda.entity.Batch;
import com.tienda.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final SupplierProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           StoreOwnerRepository storeOwnerRepository,
                           SupplierRepository supplierRepository,
                           UnitOfMeasureRepository unitRepository,
                           SupplierProductRepository productRepository,
                           BatchRepository batchRepository,
                           StoreInventoryRepository storeInventoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.storeOwnerRepository = storeOwnerRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.storeInventoryRepository = storeInventoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info(">>> DataInitializer iniciando...");

        log.info("Poblando datos iniciales...");
        String encodedPass = passwordEncoder.encode("123456");

        AdminUser admin = null;
        StoreOwner store = null;
        Supplier supplier = null;

        // Admin
        try {
            admin = new AdminUser();
            admin.setEmail("admin@tienda.com");
            admin.setPassword(encodedPass);
            admin.setFullName("Administrador");
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("  Admin creado.");
        } catch (Exception e) {
            log.warn("  Error creando admin: {}", e.getMessage(), e);
        }

        // Store
        try {
            store = new StoreOwner();
            store.setEmail("tienda@tienda.com");
            store.setPassword(encodedPass);
            store.setFullName("Tienda Principal");
            store.setRole("ROLE_STORE");
            store.setEnabled(true);
            store.setStoreName("Tienda Principal");
            store.setAddress("Av. Principal 123");
            storeOwnerRepository.save(store);
            log.info("  Tienda creada.");
        } catch (Exception e) {
            log.warn("  Error creando tienda: {}", e.getMessage(), e);
        }

        // Supplier
        try {
            supplier = new Supplier();
            supplier.setEmail("proveedor@tienda.com");
            supplier.setPassword(encodedPass);
            supplier.setFullName("Proveedor Principal");
            supplier.setRole("ROLE_SUPPLIER");
            supplier.setEnabled(true);
            supplier.setCompanyName("Distribuidora Principal");
            supplier.setContactPhone("555-0100");
            supplier.setAddress("Calle Proveedores 456");
            supplierRepository.save(supplier);
            log.info("  Proveedor creado.");
        } catch (Exception e) {
            log.warn("  Error creando proveedor: {}", e.getMessage(), e);
        }

        UnitOfMeasure uPza = null, uKg = null, uCaja = null, uLitro = null;

        // Units of measure
        try {
            uPza = unitRepository.save(buildUnit("Pieza", "pza"));
            uKg = unitRepository.save(buildUnit("Kilogramo", "kg"));
            uCaja = unitRepository.save(buildUnit("Caja", "caja"));
            uLitro = unitRepository.save(buildUnit("Litro", "L"));
            log.info("  Unidades de medida creadas.");
        } catch (Exception e) {
            log.warn("  Error creando unidades: {}", e.getMessage());
        }

        SupplierProduct p1 = null, p2 = null, p3 = null, p4 = null;

        // Products
        try {
            if (supplier != null && uPza != null && uLitro != null && uCaja != null) {
                p1 = productRepository.save(SupplierProduct.builder()
                        .name("Arroz 1kg").creationDate(LocalDate.now())
                        .basePrice(new BigDecimal("25.50")).minStock(10).maxStock(100)
                        .supplier(supplier).unit(uPza).build());
                p2 = productRepository.save(SupplierProduct.builder()
                        .name("Frijol 1kg").creationDate(LocalDate.now())
                        .basePrice(new BigDecimal("32.00")).minStock(10).maxStock(80)
                        .supplier(supplier).unit(uPza).build());
                p3 = productRepository.save(SupplierProduct.builder()
                        .name("Aceite 1L").creationDate(LocalDate.now())
                        .basePrice(new BigDecimal("45.00")).minStock(5).maxStock(50)
                        .supplier(supplier).unit(uLitro).build());
                p4 = productRepository.save(SupplierProduct.builder()
                        .name("Huevo (caja 30pzas)").creationDate(LocalDate.now())
                        .basePrice(new BigDecimal("72.00")).minStock(3).maxStock(20)
                        .supplier(supplier).unit(uCaja).build());
                log.info("  Productos creados.");
            }
        } catch (Exception e) {
            log.warn("  Error creando productos: {}", e.getMessage());
        }

        // Batches
        try {
            if (p1 != null) batchRepository.save(Batch.builder().quantity(50)
                    .expirationDate(LocalDate.now().plusMonths(12))
                    .purchasePrice(new BigDecimal("23.00")).supplierProduct(p1).build());
            if (p2 != null) batchRepository.save(Batch.builder().quantity(40)
                    .expirationDate(LocalDate.now().plusMonths(10))
                    .purchasePrice(new BigDecimal("28.00")).supplierProduct(p2).build());
            if (p3 != null) batchRepository.save(Batch.builder().quantity(30)
                    .expirationDate(LocalDate.now().plusMonths(8))
                    .purchasePrice(new BigDecimal("40.00")).supplierProduct(p3).build());
            if (p4 != null) batchRepository.save(Batch.builder().quantity(15)
                    .expirationDate(LocalDate.now().plusWeeks(3))
                    .purchasePrice(new BigDecimal("65.00")).supplierProduct(p4).build());
            log.info("  Lotes creados.");
        } catch (Exception e) {
            log.warn("  Error creando lotes: {}", e.getMessage());
        }

        // Store inventory
        try {
            if (store != null && p1 != null) storeInventoryRepository.save(StoreInventory.builder()
                    .storeOwner(store).supplierProduct(p1).quantity(30).build());
            if (store != null && p2 != null) storeInventoryRepository.save(StoreInventory.builder()
                    .storeOwner(store).supplierProduct(p2).quantity(25).build());
            if (store != null && p3 != null) storeInventoryRepository.save(StoreInventory.builder()
                    .storeOwner(store).supplierProduct(p3).quantity(15).build());
            if (store != null && p4 != null) storeInventoryRepository.save(StoreInventory.builder()
                    .storeOwner(store).supplierProduct(p4).quantity(8).build());
            log.info("  Inventario creado.");
        } catch (Exception e) {
            log.warn("  Error creando inventario: {}", e.getMessage());
        }

        log.info("Datos iniciales poblados (con posibles omisiones por errores).");
    }

    private UnitOfMeasure buildUnit(String name, String abbreviation) {
        UnitOfMeasure u = new UnitOfMeasure();
        u.setName(name);
        u.setAbbreviation(abbreviation);
        return u;
    }
}
