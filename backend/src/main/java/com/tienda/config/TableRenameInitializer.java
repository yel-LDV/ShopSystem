package com.tienda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile("none")
public class TableRenameInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TableRenameInitializer.class);

    private static final Map<String, String> RENAME_MAP = new LinkedHashMap<>();

    static {
        RENAME_MAP.put("users", "usuario");
        RENAME_MAP.put("admin_user", "admin_usuario");
        RENAME_MAP.put("store_owner", "dueno_tienda");
        RENAME_MAP.put("supplier", "proveedor");
        RENAME_MAP.put("unit_of_measure", "unidad_medida");
        RENAME_MAP.put("registration_request", "solicitud_registro");
        RENAME_MAP.put("supplier_product", "producto");
        RENAME_MAP.put("batch", "lote");
        RENAME_MAP.put("store_inventory", "inventario");
        RENAME_MAP.put("orders", "orden_compra");
        RENAME_MAP.put("order_item", "detalle_orden_compra");
        RENAME_MAP.put("message", "mensaje");
        RENAME_MAP.put("notification", "notificacion");
        RENAME_MAP.put("sale", "venta");
        RENAME_MAP.put("sale_item", "detalle_venta");
        RENAME_MAP.put("audit_log", "registro_auditoria");
        RENAME_MAP.put("price_history", "historial_precio");
        RENAME_MAP.put("email_queue", "cola_correo");
    }

    private final DataSource dataSource;

    public TableRenameInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        boolean oldTablesExist = tableExists(jdbc, "users");
        boolean newTablesExist = tableExists(jdbc, "usuario");

        if (!oldTablesExist && newTablesExist) {
            log.info("Tablas ya estan en español. No se requiere renombre.");
            return;
        }

        if (!oldTablesExist && !newTablesExist) {
            log.info("Instalación limpia. Hibernate creará las tablas con los nombres correctos.");
            return;
        }

        if (oldTablesExist && newTablesExist) {
            log.info("Ambos esquemas detectados. Renombrando tablas antiguas y eliminando nuevas vacías...");
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("SET REFERENTIAL_INTEGRITY FALSE");

                for (Map.Entry<String, String> entry : RENAME_MAP.entrySet()) {
                    String oldName = entry.getKey();
                    String newName = entry.getValue();

                    try {
                        conn.createStatement().execute("DROP TABLE IF EXISTS " + newName);
                    } catch (Exception ignored) {
                    }

                    try {
                        conn.createStatement().execute("ALTER TABLE " + oldName + " RENAME TO " + newName);
                        log.info("  {} → {}", oldName, newName);
                    } catch (Exception e) {
                        log.debug("  No se pudo renombrar {}: {}", oldName, e.getMessage());
                    }
                }

                conn.createStatement().execute("SET REFERENTIAL_INTEGRITY TRUE");
                log.info("Renombre de tablas H2 completado.");
            } catch (Exception e) {
                log.error("Error renombrando tablas H2: {}", e.getMessage(), e);
            }
            return;
        }

        if (oldTablesExist && !newTablesExist) {
            log.info("Tablas antiguas detectadas. Renombrando a español...");
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute("SET REFERENTIAL_INTEGRITY FALSE");

                for (Map.Entry<String, String> entry : RENAME_MAP.entrySet()) {
                    String oldName = entry.getKey();
                    String newName = entry.getValue();

                    try {
                        conn.createStatement().execute("ALTER TABLE " + oldName + " RENAME TO " + newName);
                        log.info("  {} → {}", oldName, newName);
                    } catch (Exception e) {
                        log.debug("  No se pudo renombrar {}: {}", oldName, e.getMessage());
                    }
                }

                conn.createStatement().execute("SET REFERENTIAL_INTEGRITY TRUE");
                log.info("Renombre de tablas H2 completado.");
            } catch (Exception e) {
                log.error("Error renombrando tablas H2: {}", e.getMessage(), e);
            }
        }
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                Integer.class,
                tableName.toUpperCase()
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
