-- Flyway V4: Remove SKU column and index from supplier_product
ALTER TABLE supplier_product DROP INDEX IF EXISTS uk_sku;
ALTER TABLE supplier_product DROP COLUMN IF EXISTS sku;
