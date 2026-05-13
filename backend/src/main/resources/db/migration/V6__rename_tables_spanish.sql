-- ==============================================
-- Flyway V6: Renombrar tablas a español (ER)
-- ==============================================

-- Usuarios
ALTER TABLE users RENAME TO usuario;
ALTER TABLE admin_user RENAME TO admin_usuario;
ALTER TABLE store_owner RENAME TO dueno_tienda;
ALTER TABLE supplier RENAME TO proveedor;

-- Catálogos
ALTER TABLE unit_of_measure RENAME TO unidad_medida;
ALTER TABLE registration_request RENAME TO solicitud_registro;

-- Productos e inventario
ALTER TABLE supplier_product RENAME TO producto;
ALTER TABLE batch RENAME TO lote;
ALTER TABLE store_inventory RENAME TO inventario;

-- Órdenes de compra
ALTER TABLE orders RENAME TO orden_compra;
ALTER TABLE order_item RENAME TO detalle_orden_compra;

-- Tickets y mensajes (ticket no cambia)
ALTER TABLE message RENAME TO mensaje;

-- Ventas
ALTER TABLE sale RENAME TO venta;
ALTER TABLE sale_item RENAME TO detalle_venta;

-- Notificaciones
ALTER TABLE notification RENAME TO notificacion;

-- Auditoría y registros
ALTER TABLE audit_log RENAME TO registro_auditoria;
ALTER TABLE price_history RENAME TO historial_precio;
ALTER TABLE email_queue RENAME TO cola_correo;
