package com.library360.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes Render and cloud-provided DATABASE_URL (e.g. postgres://user:pass@host:port/dbname)
 * into standard Spring datasource properties (spring.datasource.url, username, password, driver-class-name).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            databaseUrl = System.getenv("DATABASE_URL");
        }

        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        databaseUrl = databaseUrl.trim();

        // If it starts with postgres:// or postgresql:// (without jdbc: prefix)
        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            try {
                // Replace prefix with http:// so java.net.URI parses authority, user-info, host and port cleanly
                String normalized = databaseUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = new URI(normalized);

                String userInfo = uri.getUserInfo();
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    path = path.substring(1);
                }

                Map<String, Object> props = new HashMap<>();

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    props.put("spring.datasource.username", parts[0]);
                    props.put("spring.datasource.password", parts[1]);
                } else if (userInfo != null) {
                    props.put("spring.datasource.username", userInfo);
                }

                String query = uri.getQuery();
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(path == null ? "" : path);

                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                } else {
                    // Default to require/prefer ssl for cloud postgres
                    jdbcUrl.append("?sslmode=prefer");
                }

                props.put("spring.datasource.url", jdbcUrl.toString());
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrlProperties", props));
            } catch (Exception e) {
                // Fallback to basic string prefix replacement if uri structure is irregular
                String jdbcUrl = databaseUrl.replaceFirst("^postgres://", "jdbc:postgresql://")
                                            .replaceFirst("^postgresql://", "jdbc:postgresql://");
                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrlProperties", props));
            }
        }
    }
}
