-- ==============================================
-- Flyway V1: Esquema inicial
-- ==============================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    enabled BOOLEAN DEFAULT FALSE,
    role VARCHAR(50),
    time_zone VARCHAR(50) DEFAULT 'America/Mexico_City'
);

CREATE TABLE IF NOT EXISTS store_owner (
    id BIGINT PRIMARY KEY,
    store_name VARCHAR(255),
    address VARCHAR(500),
    favorite_supplier_id BIGINT,
    FOREIGN KEY (id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT PRIMARY KEY,
    company_name VARCHAR(255),
    contact_phone VARCHAR(50),
    emergency_email VARCHAR(255),
    address VARCHAR(500),
    FOREIGN KEY (id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS unit_of_measure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    abbreviation VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS supplier_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    creation_date DATE,
    expiration_date DATE,
    base_price DECIMAL(12,2),
    min_stock INT DEFAULT 0,
    max_stock INT DEFAULT 0,
    supplier_id BIGINT,
    unit_id BIGINT,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    FOREIGN KEY (unit_id) REFERENCES unit_of_measure(id)
);

CREATE TABLE IF NOT EXISTS batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    expiration_date DATE,
    purchase_price DECIMAL(12,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    supplier_product_id BIGINT,
    FOREIGN KEY (supplier_product_id) REFERENCES supplier_product(id)
);

CREATE TABLE IF NOT EXISTS store_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_owner_id BIGINT,
    supplier_product_id BIGINT,
    quantity INT NOT NULL DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_owner_id) REFERENCES store_owner(id),
    FOREIGN KEY (supplier_product_id) REFERENCES supplier_product(id),
    UNIQUE KEY uk_store_product (store_owner_id, supplier_product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_owner_id BIGINT,
    supplier_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_automatic BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME,
    received_at DATETIME,
    rejection_reason VARCHAR(500),
    FOREIGN KEY (store_owner_id) REFERENCES store_owner(id),
    FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    supplier_product_id BIGINT,
    quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (supplier_product_id) REFERENCES supplier_product(id)
);

CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    voting_end_date DATETIME,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    final_resolution VARCHAR(50),
    discount_percentage INT DEFAULT 0,
    store_owner_id BIGINT,
    supplier_id BIGINT,
    order_id BIGINT,
    store_owner_vote VARCHAR(50),
    supplier_vote VARCHAR(50),
    FOREIGN KEY (store_owner_id) REFERENCES store_owner(id),
    FOREIGN KEY (supplier_id) REFERENCES supplier(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT,
    sender_role VARCHAR(50),
    content VARCHAR(2000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES ticket(id)
);

CREATE TABLE IF NOT EXISTS registration_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50),
    store_name VARCHAR(255),
    store_address VARCHAR(500),
    company_name VARCHAR(255),
    contact_phone VARCHAR(50),
    emergency_email VARCHAR(255),
    address VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    action VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id BIGINT,
    old_value VARCHAR(5000),
    new_value VARCHAR(5000),
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    message VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50),
    reference_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_product_id BIGINT,
    old_price DECIMAL(12,2),
    new_price DECIMAL(12,2),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_product_id) REFERENCES supplier_product(id)
);

CREATE TABLE IF NOT EXISTS email_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(255),
    subject VARCHAR(500),
    body VARCHAR(5000),
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000)
);
