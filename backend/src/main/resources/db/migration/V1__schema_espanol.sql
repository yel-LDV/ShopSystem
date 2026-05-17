-- ==============================================
-- V1: Schema completo en español (MariaDB)
-- ==============================================

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    enabled BOOLEAN DEFAULT FALSE,
    role VARCHAR(50),
    time_zone VARCHAR(50) DEFAULT 'America/Mexico_City'
);

CREATE TABLE IF NOT EXISTS admin_usuario (
    id BIGINT PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS dueno_tienda (
    id BIGINT PRIMARY KEY,
    store_name VARCHAR(255),
    address VARCHAR(500),
    favorite_supplier_id BIGINT,
    FOREIGN KEY (id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS proveedor (
    id BIGINT PRIMARY KEY,
    company_name VARCHAR(255),
    contact_phone VARCHAR(50),
    emergency_email VARCHAR(255),
    address VARCHAR(500),
    FOREIGN KEY (id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS unidad_medida (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    abbreviation VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS solicitud_registro (
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

CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100),
    creation_date DATE,
    expiration_date DATE,
    base_price DECIMAL(12,2),
    min_stock INT DEFAULT 0,
    max_stock INT DEFAULT 0,
    supplier_id BIGINT,
    unit_id BIGINT,
    FOREIGN KEY (supplier_id) REFERENCES proveedor(id),
    FOREIGN KEY (unit_id) REFERENCES unidad_medida(id)
);

CREATE TABLE IF NOT EXISTS lote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    expiration_date DATE,
    purchase_price DECIMAL(12,2),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    supplier_product_id BIGINT,
    FOREIGN KEY (supplier_product_id) REFERENCES producto(id)
);

CREATE TABLE IF NOT EXISTS inventario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_owner_id BIGINT,
    supplier_product_id BIGINT,
    quantity INT NOT NULL DEFAULT 0,
    min_stock INT DEFAULT 0,
    max_stock INT DEFAULT 0,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_owner_id) REFERENCES dueno_tienda(id),
    FOREIGN KEY (supplier_product_id) REFERENCES producto(id)
);

CREATE TABLE IF NOT EXISTS orden_compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_owner_id BIGINT,
    supplier_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_automatic BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME,
    received_at DATETIME,
    rejection_reason VARCHAR(500),
    FOREIGN KEY (store_owner_id) REFERENCES dueno_tienda(id),
    FOREIGN KEY (supplier_id) REFERENCES proveedor(id)
);

CREATE TABLE IF NOT EXISTS detalle_orden_compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    supplier_product_id BIGINT,
    quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2),
    batch_id BIGINT,
    FOREIGN KEY (order_id) REFERENCES orden_compra(id),
    FOREIGN KEY (supplier_product_id) REFERENCES producto(id)
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
    admin_vote VARCHAR(50),
    admin_id BIGINT,
    proposed_price DECIMAL(12,2),
    price_proposed_by VARCHAR(50),
    negotiation_status VARCHAR(50) DEFAULT 'NONE',
    FOREIGN KEY (store_owner_id) REFERENCES dueno_tienda(id),
    FOREIGN KEY (supplier_id) REFERENCES proveedor(id),
    FOREIGN KEY (order_id) REFERENCES orden_compra(id),
    FOREIGN KEY (admin_id) REFERENCES admin_usuario(id)
);

CREATE TABLE IF NOT EXISTS mensaje (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT,
    sender_role VARCHAR(50),
    content VARCHAR(2000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES ticket(id)
);

CREATE TABLE IF NOT EXISTS venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_owner_id BIGINT,
    sale_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(12,2),
    FOREIGN KEY (store_owner_id) REFERENCES dueno_tienda(id)
);

CREATE TABLE IF NOT EXISTS detalle_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    supplier_product_id BIGINT,
    quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2),
    FOREIGN KEY (sale_id) REFERENCES venta(id),
    FOREIGN KEY (supplier_product_id) REFERENCES producto(id)
);

CREATE TABLE IF NOT EXISTS notificacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    message VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50),
    reference_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS registro_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    action VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id BIGINT,
    old_value VARCHAR(5000),
    new_value VARCHAR(5000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historial_precio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_product_id BIGINT,
    old_price DECIMAL(12,2),
    new_price DECIMAL(12,2),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_product_id) REFERENCES producto(id)
);

CREATE TABLE IF NOT EXISTS cola_correo (
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

-- Uniques
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_name ON producto (supplier_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS uk_store_product ON inventario (store_owner_id, supplier_product_id);
