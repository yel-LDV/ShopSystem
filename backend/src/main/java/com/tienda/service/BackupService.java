package com.tienda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPass;

    @Value("${app.backup.dir}")
    private String backupDir;

    @Value("${app.backup.key}")
    private String backupKey;

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledBackup() {
        try {
            String filename = "backup_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".enc";
            performBackup(filename);
            log.info("Backup diario completado: {}", filename);
        } catch (Exception e) {
            log.error("Error en backup diario: {}", e.getMessage(), e);
        }
    }

    public String performBackup(String filename) throws Exception {
        Files.createDirectories(Paths.get(backupDir));

        String sql = generateJdbcDump();

        byte[] compressed = compress(sql);
        byte[] encrypted = encrypt(compressed, backupKey);

        Path backupFile = Paths.get(backupDir, filename);
        Files.write(backupFile, encrypted);

        log.info("Backup creado: {} ({} bytes)", backupFile, encrypted.length);
        return backupFile.toString();
    }

    public void restoreBackup(String filename) throws Exception {
        Path backupFile = Paths.get(backupDir, filename);
        if (!Files.exists(backupFile)) {
            backupFile = Paths.get(filename);
        }
        if (!Files.exists(backupFile)) {
            throw new RuntimeException("Archivo de backup no encontrado: " + filename);
        }

        byte[] encrypted = Files.readAllBytes(backupFile);
        byte[] compressed = decrypt(encrypted, backupKey);
        String sql = decompress(compressed);

        executeSql(sql);
        log.info("Backup restaurado exitosamente: {}", filename);
    }

    private String generateJdbcDump() throws Exception {
        StringBuilder dump = new StringBuilder();
        dump.append("-- Backup generado via JDBC: ").append(LocalDateTime.now()).append("\n\n");

        try (Connection conn = DriverManager.getConnection(datasourceUrl, dbUser, dbPass)) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> tables = new ArrayList<>();

            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (!tableName.startsWith("flyway_")) {
                        tables.add(tableName);
                    }
                }
            }

            for (String table : tables) {
                dump.append("-- Table: ").append(table).append("\n");
                dump.append(generateTableDump(conn, table)).append("\n\n");
            }
        }

        return dump.toString();
    }

    private String generateTableDump(Connection conn, String tableName) throws SQLException {
        StringBuilder dump = new StringBuilder();

        List<String> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 1")) {
            ResultSetMetaData rsMeta = rs.getMetaData();
            for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                columns.add(rsMeta.getColumnName(i));
            }
        } catch (SQLException e) {
            return "-- Could not read " + tableName + ": " + e.getMessage() + "\n";
        }

        if (columns.isEmpty()) return "";

        String colList = String.join(", ", columns);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
            ResultSetMetaData rsMeta = rs.getMetaData();
            while (rs.next()) {
                StringBuilder values = new StringBuilder();
                for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                    if (i > 1) values.append(", ");
                    String val = rs.getString(i);
                    if (val == null) {
                        values.append("NULL");
                    } else {
                        values.append("'").append(val.replace("'", "''")).append("'");
                    }
                }
                dump.append("INSERT INTO ").append(tableName)
                    .append(" (").append(colList).append(") VALUES (")
                    .append(values).append(");\n");
            }
        }

        return dump.toString();
    }

    private void executeSql(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(datasourceUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception e) {
                        log.warn("Error ejecutando: {} - {}",
                                trimmed.substring(0, Math.min(50, trimmed.length())), e.getMessage());
                    }
                }
            }
        }
    }

    private byte[] compress(String data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bos);
        gzip.write(data.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        return bos.toByteArray();
    }

    private String decompress(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bis);
        return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
    }

    private byte[] encrypt(byte[] data, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        keyBytes = sha.digest(keyBytes);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data, String key) throws Exception {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        keyBytes = sha.digest(keyBytes);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        return cipher.doFinal(data);
    }
}
