package com.tienda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MariaDbConfig {

    @Value("${app.mariadb.host:localhost}")
    private String host;

    @Value("${app.mariadb.port:3307}")
    private String port;

    @Value("${app.mariadb.name:market}")
    private String dbName;

    @Value("${app.mariadb.user:root}")
    private String user;

    @Value("${app.mariadb.password:}")
    private String password;

    @Bean
    @Conditional(MariaDbAvailableCondition.class)
    public DataSource mariaDbDataSource() {
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        return DataSourceBuilder.create()
                .url(url)
                .username(user)
                .password(password)
                .driverClassName("org.mariadb.jdbc.Driver")
                .build();
    }
}
