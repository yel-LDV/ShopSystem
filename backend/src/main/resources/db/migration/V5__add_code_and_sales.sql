-- ==============================================
-- Flyway V5: code column + sale tables
-- ==============================================
ALTER TABLE supplier_product ADD COLUMN IF NOT EXISTS code VARCHAR(100);

CREATE TABLE IF NOT EXISTS sale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_owner_id BIGINT,
    sale_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(12,2),
    FOREIGN KEY (store_owner_id) REFERENCES store_owner(id)
);

CREATE TABLE IF NOT EXISTS sale_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    supplier_product_id BIGINT,
    quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2),
    FOREIGN KEY (sale_id) REFERENCES sale(id),
    FOREIGN KEY (supplier_product_id) REFERENCES supplier_product(id)
);

ALTER TABLE store_inventory ADD COLUMN IF NOT EXISTS min_stock INT DEFAULT 0;
ALTER TABLE store_inventory ADD COLUMN IF NOT EXISTS max_stock INT DEFAULT 0;

ALTER TABLE ticket ADD COLUMN IF NOT EXISTS admin_vote VARCHAR(50);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS admin_id BIGINT;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS proposed_price DECIMAL(12,2);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS price_proposed_by VARCHAR(50);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS negotiation_status VARCHAR(50) DEFAULT 'NONE';
