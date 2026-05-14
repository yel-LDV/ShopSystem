-- ==============================================
-- V6: Renombrar tablas a español (MariaDB syntax)
-- ==============================================

RENAME TABLE users TO usuario;
RENAME TABLE admin_user TO admin_usuario;
RENAME TABLE store_owner TO dueno_tienda;
RENAME TABLE supplier TO proveedor;

RENAME TABLE unit_of_measure TO unidad_medida;
RENAME TABLE registration_request TO solicitud_registro;

RENAME TABLE supplier_product TO producto;
RENAME TABLE batch TO lote;
RENAME TABLE store_inventory TO inventario;

RENAME TABLE orders TO orden_compra;
RENAME TABLE order_item TO detalle_orden_compra;

RENAME TABLE message TO mensaje;

RENAME TABLE sale TO venta;
RENAME TABLE sale_item TO detalle_venta;

RENAME TABLE notification TO notificacion;

RENAME TABLE audit_log TO registro_auditoria;
RENAME TABLE price_history TO historial_precio;
RENAME TABLE email_queue TO cola_correo;
