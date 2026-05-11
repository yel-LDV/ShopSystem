-- ==============================================
-- Flyway V2: Seed admin user
-- ==============================================
INSERT INTO users (email, password, full_name, enabled, role)
SELECT 'admin@tienda.com', '$2a$10$kbfDR8Mb1ASJPsBD4vQwKOZlJBpGW1QWoT7WOVWGB9r/I0/B8kLQe', 'Administrador', TRUE, 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@tienda.com');

INSERT INTO admin_user (id)
SELECT id FROM users WHERE email = 'admin@tienda.com'
AND NOT EXISTS (SELECT 1 FROM admin_user WHERE id = (SELECT id FROM users WHERE email = 'admin@tienda.com'));
