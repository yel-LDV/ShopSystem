package com.tienda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class MariaDbSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MariaDbSyncService.class);

    private static final List<String> TABLE_ORDER = List.of(
            "users",
            "admin_user",
            "store_owner",
            "supplier",
            "unit_of_measure",
            "registration_request",
            "supplier_product",
            "batch",
            "store_inventory",
            "orders",
            "order_item",
            "ticket",
            "message",
            "sale",
            "sale_item",
            "notification",
            "audit_log",
            "price_history",
            "email_queue"
    );

    private final DataSource h2DataSource;

    @Autowired(required = false)
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
        JdbcTemplate h2 = new JdbcTemplate(h2DataSource);
        JdbcTemplate maria = new JdbcTemplate(mariaDbDataSource);

        executeSchema(maria);
        copyAllData(h2, maria);
    }

    private void executeSchema(JdbcTemplate maria) throws Exception {
        log.info("Ejecutando schema DDL en MariaDB...");
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_schema.sql");
        String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String[] statements = sql.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            try {
                maria.execute(trimmed);
            } catch (Exception e) {
                log.debug("Ignorando error esperado en DDL: {}", e.getMessage());
            }
        }
        log.info("Schema DDL ejecutado en MariaDB.");
    }

    private void copyAllData(JdbcTemplate h2, JdbcTemplate maria) {
        log.info("Copiando datos de H2 -> MariaDB...");

        try (Connection mariaConn = mariaDbDataSource.getConnection()) {
            mariaConn.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");

            for (String table : TABLE_ORDER) {
                try {
                    copyTable(h2, maria, table);
                } catch (Exception e) {
                    log.warn("Error copiando tabla '{}': {}", table, e.getMessage());
                }
            }

            mariaConn.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");
        } catch (Exception e) {
            log.error("Error en sync FK: {}", e.getMessage());
        }

        log.info("Datos copiados H2 -> MariaDB completado.");
    }

    private void copyTable(JdbcTemplate h2, JdbcTemplate maria, String tableName) {
        List<Map<String, Object>> rows = h2.queryForList("SELECT * FROM " + tableName);
        if (rows.isEmpty()) {
            log.debug("  {}: 0 filas", tableName);
            return;
        }

        maria.execute("DELETE FROM " + tableName);

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

            maria.update("INSERT INTO " + tableName + " (" + cols + ") VALUES (" + vals + ")", params.toArray());
        }

        log.info("  {}: {} filas copiadas", tableName, rows.size());
    }
}
