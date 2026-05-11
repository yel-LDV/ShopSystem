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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

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
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        String encodedPass = passwordEncoder.encode("123456");

        // Admin user
        AdminUser admin = new AdminUser();
        admin.setEmail("admin@tienda.com");
        admin.setPassword(encodedPass);
        admin.setFullName("Administrador");
        admin.setRole("ROLE_ADMIN");
        admin.setEnabled(true);
        userRepository.save(admin);

        // Store owner
        StoreOwner store = new StoreOwner();
        store.setEmail("tienda@tienda.com");
        store.setPassword(encodedPass);
        store.setFullName("Tienda Principal");
        store.setRole("ROLE_STORE");
        store.setEnabled(true);
        store.setStoreName("Tienda Principal");
        store.setAddress("Av. Principal 123");
        storeOwnerRepository.save(store);

        // Supplier
        Supplier supplier = new Supplier();
        supplier.setEmail("proveedor@tienda.com");
        supplier.setPassword(encodedPass);
        supplier.setFullName("Proveedor Principal");
        supplier.setRole("ROLE_SUPPLIER");
        supplier.setEnabled(true);
        supplier.setCompanyName("Distribuidora Principal");
        supplier.setContactPhone("555-0100");
        supplier.setAddress("Calle Proveedores 456");
        supplierRepository.save(supplier);

        // Units of measure
        UnitOfMeasure uPza = new UnitOfMeasure(null, "Pieza", "pza");
        UnitOfMeasure uKg = new UnitOfMeasure(null, "Kilogramo", "kg");
        UnitOfMeasure uCaja = new UnitOfMeasure(null, "Caja", "caja");
        UnitOfMeasure uLitro = new UnitOfMeasure(null, "Litro", "L");
        uPza = unitRepository.save(uPza);
        uKg = unitRepository.save(uKg);
        uCaja = unitRepository.save(uCaja);
        uLitro = unitRepository.save(uLitro);

        // Products
        SupplierProduct p1 = SupplierProduct.builder()
                .name("Arroz 1kg")
                .creationDate(LocalDate.now())
                .basePrice(new BigDecimal("25.50"))
                .minStock(10)
                .maxStock(100)
                .supplier(supplier)
                .unit(uPza)
                .build();
        p1 = productRepository.save(p1);

        SupplierProduct p2 = SupplierProduct.builder()
                .name("Frijol 1kg")
                .creationDate(LocalDate.now())
                .basePrice(new BigDecimal("32.00"))
                .minStock(10)
                .maxStock(80)
                .supplier(supplier)
                .unit(uPza)
                .build();
        p2 = productRepository.save(p2);

        SupplierProduct p3 = SupplierProduct.builder()
                .name("Aceite 1L")
                .creationDate(LocalDate.now())
                .basePrice(new BigDecimal("45.00"))
                .minStock(5)
                .maxStock(50)
                .supplier(supplier)
                .unit(uLitro)
                .build();
        p3 = productRepository.save(p3);

        SupplierProduct p4 = SupplierProduct.builder()
                .name("Huevo (caja 30pzas)")
                .creationDate(LocalDate.now())
                .basePrice(new BigDecimal("72.00"))
                .minStock(3)
                .maxStock(20)
                .supplier(supplier)
                .unit(uCaja)
                .build();
        p4 = productRepository.save(p4);

        // Batches
        batchRepository.save(Batch.builder()
                .quantity(50).expirationDate(LocalDate.now().plusMonths(12))
                .purchasePrice(new BigDecimal("23.00")).supplierProduct(p1).build());
        batchRepository.save(Batch.builder()
                .quantity(40).expirationDate(LocalDate.now().plusMonths(10))
                .purchasePrice(new BigDecimal("28.00")).supplierProduct(p2).build());
        batchRepository.save(Batch.builder()
                .quantity(30).expirationDate(LocalDate.now().plusMonths(8))
                .purchasePrice(new BigDecimal("40.00")).supplierProduct(p3).build());
        batchRepository.save(Batch.builder()
                .quantity(15).expirationDate(LocalDate.now().plusWeeks(3))
                .purchasePrice(new BigDecimal("65.00")).supplierProduct(p4).build());

        // Store inventory
        storeInventoryRepository.save(StoreInventory.builder()
                .storeOwner(store).supplierProduct(p1).quantity(30).build());
        storeInventoryRepository.save(StoreInventory.builder()
                .storeOwner(store).supplierProduct(p2).quantity(25).build());
        storeInventoryRepository.save(StoreInventory.builder()
                .storeOwner(store).supplierProduct(p3).quantity(15).build());
        storeInventoryRepository.save(StoreInventory.builder()
                .storeOwner(store).supplierProduct(p4).quantity(8).build());
    }
}
