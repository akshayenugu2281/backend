package com.library360.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
