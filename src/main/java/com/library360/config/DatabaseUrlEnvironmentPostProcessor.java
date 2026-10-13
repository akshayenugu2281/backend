package com.library360.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes Render, Docker, and cloud-provided database URLs into valid PostgreSQL JDBC URLs
 * by stripping embedded user/password credentials from the URL and setting them into
 * standard Spring datasource username and password properties.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    // Matches: [jdbc:]postgres[ql]://[user[:pass]@]host[:port][/dbname][?query]
    private static final Pattern PG_URL_PATTERN = Pattern.compile(
            "^(?:jdbc:)?postgres(?:ql)?://(?:([^:@/]+)(?::([^@/]*))?@)?([^:/?#]+)(?::(\\d+))?(?:/([^?#]*))?(?:\\?(.*))?$"
    );

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

        // 1. Resolve explicit user/password if set
        String explicitUser = findProperty(environment, USER_KEYS);
        String explicitPassword = findProperty(environment, PASSWORD_KEYS);

        // 2. Resolve database URL from any standard environment variable
        String rawUrl = findProperty(environment, URL_KEYS);

        if (rawUrl != null && !rawUrl.isBlank()) {
            rawUrl = rawUrl.trim();
            Matcher matcher = PG_URL_PATTERN.matcher(rawUrl);

            if (matcher.matches()) {
                String urlUser = matcher.group(1);
                String urlPass = matcher.group(2);
                String host = matcher.group(3);
                String portStr = matcher.group(4);
                String dbName = matcher.group(5);
                String query = matcher.group(6);

                int port = (portStr != null && !portStr.isBlank()) ? Integer.parseInt(portStr) : 5432;
                if (dbName == null || dbName.isBlank()) {
                    dbName = "library360_database";
                }

                // Strip credentials completely from JDBC URL (org.postgresql.Driver rejects user:pass@ inside jdbcUrl)
                StringBuilder cleanJdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append("/")
                        .append(dbName);

                if (query != null && !query.isBlank()) {
                    cleanJdbcUrl.append("?").append(query);
                } else {
                    cleanJdbcUrl.append("?sslmode=prefer");
                }

                String finalUser = (explicitUser != null && !explicitUser.isBlank()) ? explicitUser : urlUser;
                String finalPassword = (explicitPassword != null && !explicitPassword.isBlank()) ? explicitPassword : urlPass;

                if (finalUser != null && !finalUser.isBlank()) {
                    props.put("spring.datasource.username", finalUser);
                }
                if (finalPassword != null && !finalPassword.isBlank()) {
                    props.put("spring.datasource.password", finalPassword);
                }

                props.put("spring.datasource.url", cleanJdbcUrl.toString());
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

                System.out.println(">>> [Library360] Normalized PostgreSQL JDBC URL: " + cleanJdbcUrl + " for user: " + finalUser);
            } else if (rawUrl.startsWith("jdbc:postgresql:")) {
                // If it's already a JDBC URL, ensure any accidentally included credentials before '@' are cleaned
                if (rawUrl.contains("@")) {
                    String cleaned = rawUrl.replaceFirst("jdbc:postgresql://[^@]+@", "jdbc:postgresql://");
                    props.put("spring.datasource.url", cleaned);
                } else {
                    props.put("spring.datasource.url", rawUrl);
                }
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            }
        }

        if (explicitUser != null && !explicitUser.isBlank()) {
            props.put("spring.datasource.username", explicitUser);
        }
        if (explicitPassword != null && !explicitPassword.isBlank()) {
            props.put("spring.datasource.password", explicitPassword);
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
