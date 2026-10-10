package com.streamsphere.central.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderPostgresDataSourceConfigTest {

    @Test
    void convertsRenderPostgresConnectionStringToJdbcUrl() {
        assertEquals(
                "jdbc:postgresql://dpg-example.internal:5432/streamsphere?sslmode=require",
                RenderPostgresDataSourceConfig.toJdbcUrl(
                        "postgresql://user:password@dpg-example.internal:5432/streamsphere?sslmode=require"));
    }

    @Test
    void rejectsConnectionStringsWithUnsupportedSchemes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RenderPostgresDataSourceConfig.toJdbcUrl("mysql://db.example.test/database"));
    }

    @Test
    void configuresDataSourceUsingRenderConnectionStringAndDatabaseCredentials() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:postgresql://localhost:5432/local");
        properties.setUsername("render-user");
        properties.setPassword("render-password");
        MockEnvironment environment = new MockEnvironment().withProperty(
                "DB_CONNECTION_STRING",
                "postgresql://embedded-user:embedded-password@dpg-example.internal:5432/streamsphere");

        DataSource dataSource = new RenderPostgresDataSourceConfig().dataSource(properties, environment);
        HikariDataSource hikariDataSource = assertInstanceOf(HikariDataSource.class, dataSource);
        try {
            assertEquals("jdbc:postgresql://dpg-example.internal:5432/streamsphere", hikariDataSource.getJdbcUrl());
            assertEquals("render-user", hikariDataSource.getUsername());
            assertEquals("render-password", hikariDataSource.getPassword());
        } finally {
            hikariDataSource.close();
        }
    }
}
