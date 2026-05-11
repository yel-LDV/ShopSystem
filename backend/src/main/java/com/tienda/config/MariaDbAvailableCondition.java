package com.tienda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;

public class MariaDbAvailableCondition implements Condition {

    private static final Logger log = LoggerFactory.getLogger(MariaDbAvailableCondition.class);

    @Override
    public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
        Environment env = ctx.getEnvironment();
        String host = env.getProperty("app.mariadb.host", "localhost");
        String port = env.getProperty("app.mariadb.port", "3307");
        String user = env.getProperty("app.mariadb.user", "root");
        String password = env.getProperty("app.mariadb.password", "");
        String dbName = env.getProperty("app.mariadb.name", "market");

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName
                + "?connectTimeout=3000&createDatabaseIfNotExist=true";

        try {
            Class.forName("org.mariadb.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                boolean valid = conn.isValid(3);
                if (valid) {
                    log.info("MariaDB detectada en {}:{} — se sincronizara desde H2 (autoritativo)", host, port);
                }
                return valid;
            }
        } catch (Exception e) {
            log.warn("MariaDB NO detectada en {}:{} — usando solo H2 (respaldo local)", host, port);
            return false;
        }
    }
}
