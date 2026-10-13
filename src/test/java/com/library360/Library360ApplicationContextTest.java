package com.library360;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Library360ApplicationContextTest {
    @Test
    void applicationContextStartsWithIsolatedTestConfiguration() {
        // Context startup validates bean wiring and the JPA entity mappings without a live MySQL server.
    }
}
