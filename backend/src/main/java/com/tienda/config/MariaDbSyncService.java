package com.tienda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class MariaDbSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MariaDbSyncService.class);

    private static final List<String> TABLE_ORDER = List.of(
            "usuario",
            "admin_usuario",
            "dueno_tienda",
            "proveedor",
            "unidad_medida",
            "solicitud_registro",
            "producto",
            "lote",
            "inventario",
            "orden_compra",
            "detalle_orden_compra",
            "ticket",
            "mensaje",
            "venta",
            "detalle_venta",
            "notificacion",
            "registro_auditoria",
            "historial_precio",
            "cola_correo"
    );

    private final DataSource h2DataSource;

    @Autowired(required = false)
    @Qualifier("mariaDbDataSource")
    private DataSource mariaDbDataSource;

    public MariaDbSyncService(DataSource h2DataSource) {
        this.h2DataSource = h2DataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (mariaDbDataSource == null) {
            log.info("MariaDB no configurada. Usando solo H2 como base autoritativa.");
            return;
        }

        try {
            syncH2ToMariaDb();
            log.info("MariaDB sincronizada exitosamente desde H2 (autoritativo).");
        } catch (Exception e) {
            log.error("Error al sincronizar H2 -> MariaDB: {}", e.getMessage(), e);
        }
    }

    private void syncH2ToMariaDb() throws Exception {
        try (Connection h2Conn = h2DataSource.getConnection();
             Connection mariaConn = mariaDbDataSource.getConnection()) {

            h2Conn.setAutoCommit(true);
            mariaConn.setAutoCommit(true);

            executeSchema(mariaConn);
            copyAllData(h2Conn, mariaConn);
        }
    }

    private void executeSchema(Connection maria) throws Exception {
        log.info("Ejecutando schema DDL en MariaDB...");

        try (Statement stmt = maria.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            String[] migrations = {"db/migration/V1__initial_schema.sql", "db/migration/V6__rename_tables_spanish.sql"};
            for (String migrationPath : migrations) {
                ClassPathResource resource = new ClassPathResource(migrationPath);
                if (!resource.exists()) {
                    log.warn("Migracion no encontrada: {}", migrationPath);
                    continue;
                }
                String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                for (String rawStmt : sql.split(";")) {
                    String cleaned = stripComments(rawStmt);
                    if (cleaned.isEmpty()) continue;
                    try {
                        stmt.execute(cleaned);
                    } catch (Exception e) {
                        log.warn("Error ejecutando DDL [{}]: {}",
                                cleaned.substring(0, Math.min(80, cleaned.length())), e.getMessage());
                    }
                }
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        log.info("Schema DDL ejecutado en MariaDB.");
    }

    private String stripComments(String stmt) {
        StringBuilder sb = new StringBuilder();
        for (String line : stmt.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            sb.append(trimmed).append(" ");
        }
        return sb.toString().trim();
    }

    private void copyAllData(Connection h2Conn, Connection mariaConn) throws Exception {
        log.info("Copiando datos de H2 -> MariaDB...");

        try (Statement stmt = mariaConn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
        }

        for (String table : TABLE_ORDER) {
            try {
                copyTable(h2Conn, mariaConn, table);
            } catch (Exception e) {
                log.warn("Error copiando tabla '{}': {}", table, e.getMessage());
            }
        }

        try (Statement stmt = mariaConn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        log.info("Datos copiados H2 -> MariaDB completado.");
    }

    private void copyTable(Connection h2Conn, Connection mariaConn, String tableName) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Statement h2Stmt = h2Conn.createStatement();
             ResultSet rs = h2Stmt.executeQuery("SELECT * FROM " + tableName)) {
            var meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
                }
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            log.debug("  {}: 0 filas", tableName);
            return;
        }

        try (Statement stmt = mariaConn.createStatement()) {
            stmt.execute("DELETE FROM " + tableName);
        }

        for (Map<String, Object> row : rows) {
            StringBuilder cols = new StringBuilder();
            StringBuilder vals = new StringBuilder();
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (cols.length() > 0) {
                    cols.append(", ");
                    vals.append(", ");
                }
                cols.append(entry.getKey());
                vals.append("?");
                params.add(entry.getValue());
            }

            try (PreparedStatement ps = mariaConn.prepareStatement(
                    "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + vals + ")")) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                ps.executeUpdate();
            }
        }

        log.info("  {}: {} filas copiadas", tableName, rows.size());
    }
}
