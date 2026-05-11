-- Flyway V3: Prevent duplicate products per supplier
ALTER TABLE supplier_product ADD UNIQUE INDEX uk_supplier_name (supplier_id, name);
ALTER TABLE supplier_product ADD UNIQUE INDEX uk_sku (sku);
