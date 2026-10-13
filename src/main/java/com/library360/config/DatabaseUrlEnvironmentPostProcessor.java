package com.library360.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes Render and cloud-provided database URLs and credentials into standard
 * Spring datasource properties (spring.datasource.url, username, password, driver-class-name).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> URL_KEYS = List.of(
            "DATABASE_URL",
            "DB_URL",
            "SPRING_DATASOURCE_URL",
            "POSTGRES_URL"
    );

    private static final List<String> USER_KEYS = List.of(
            "DB_USER",
            "DB_USERNAME",
            "POSTGRES_USER",
            "POSTGRES_USERNAME",
            "SPRING_DATASOURCE_USERNAME"
    );

    private static final List<String> PASSWORD_KEYS = List.of(
            "DB_PASSWORD",
            "POSTGRES_PASSWORD",
            "SPRING_DATASOURCE_PASSWORD"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = new HashMap<>();

        // 1. Resolve explicit user / password if set under any common name
        String resolvedUser = findProperty(environment, USER_KEYS);
        if (resolvedUser != null && !resolvedUser.isBlank()) {
            props.put("spring.datasource.username", resolvedUser.trim());
        }

        String resolvedPassword = findProperty(environment, PASSWORD_KEYS);
        if (resolvedPassword != null && !resolvedPassword.isBlank()) {
            props.put("spring.datasource.password", resolvedPassword.trim());
        }

        // 2. Resolve database URL
        String rawUrl = findProperty(environment, URL_KEYS);
        if (rawUrl != null && !rawUrl.isBlank()) {
            rawUrl = rawUrl.trim();

            // Strip "jdbc:" if followed by postgres:// or postgresql:// for URI parsing
            String uriString = rawUrl;
            boolean hadJdbcPrefix = false;
            if (uriString.startsWith("jdbc:postgres://") || uriString.startsWith("jdbc:postgresql://")) {
                uriString = uriString.substring(5);
                hadJdbcPrefix = true;
            }

            if (uriString.startsWith("postgres://") || uriString.startsWith("postgresql://")) {
                try {
                    String normalized = uriString.replaceFirst("^postgres(ql)?://", "http://");
                    URI uri = new URI(normalized);

                    String userInfo = uri.getUserInfo();
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/")) {
                        path = path.substring(1);
                    }

                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        props.put("spring.datasource.username", parts[0]);
                        props.put("spring.datasource.password", parts[1]);
                    } else if (userInfo != null && !userInfo.isBlank()) {
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
                        jdbcUrl.append("?sslmode=prefer");
                    }

                    props.put("spring.datasource.url", jdbcUrl.toString());
                    props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                } catch (Exception e) {
                    String jdbcUrl = uriString.replaceFirst("^postgres://", "jdbc:postgresql://")
                                              .replaceFirst("^postgresql://", "jdbc:postgresql://");
                    props.put("spring.datasource.url", jdbcUrl);
                    props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                }
            } else if (rawUrl.startsWith("jdbc:postgresql:")) {
                props.put("spring.datasource.url", rawUrl);
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            }
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("renderNormalizedDatabaseProperties", props));
        }
    }

    private String findProperty(ConfigurableEnvironment environment, List<String> keys) {
        for (String key : keys) {
            String val = environment.getProperty(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
            val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }
}
