package com.library360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT configuration properties bound from application.yml.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
    String issuer,
    String secret,
    Duration accessTtl,
    Duration refreshTtl
) {}
