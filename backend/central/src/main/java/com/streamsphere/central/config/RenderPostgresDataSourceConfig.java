package com.streamsphere.central.config;

import java.net.URI;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class RenderPostgresDataSourceConfig {

    @Bean
    DataSource dataSource(DataSourceProperties properties, Environment environment) {
        String renderConnectionString = environment.getProperty("DB_CONNECTION_STRING");
        if (!StringUtils.hasText(renderConnectionString)) {
            return properties.initializeDataSourceBuilder().build();
        }

        return properties.initializeDataSourceBuilder()
                .url(toJdbcUrl(renderConnectionString))
                .build();
    }

    static String toJdbcUrl(String connectionString) {
        URI uri = URI.create(connectionString);
        if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
            throw new IllegalArgumentException("DB_CONNECTION_STRING must use the postgres or postgresql scheme");
        }
        if (!StringUtils.hasText(uri.getHost()) || !StringUtils.hasText(uri.getRawPath())) {
            throw new IllegalArgumentException("DB_CONNECTION_STRING must include a database host and name");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }
        return jdbcUrl.toString();
    }
}
