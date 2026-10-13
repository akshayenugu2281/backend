package com.library360.user;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserLockoutTest {
    private static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");

    @Test
    void locksAfterConfiguredNumberOfFailedAttemptsAndUnlocksAtBoundary() {
        User user = new User("student@example.edu", "hash", "Test Student");
        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(5, Duration.ofMinutes(15), NOW);
            assertFalse(user.isLocked(NOW), "Account must not lock before the threshold");
        }

        user.recordFailedLogin(5, Duration.ofMinutes(15), NOW);
        assertTrue(user.isLocked(NOW));
        assertTrue(user.isLocked(NOW.plus(Duration.ofMinutes(14))));
        assertFalse(user.isLocked(NOW.plus(Duration.ofMinutes(15))));
    }

    @Test
    void successfulLoginResetsLockoutState() {
        User user = new User("student@example.edu", "hash", "Test Student");
        user.recordFailedLogin(2, Duration.ofMinutes(15), NOW);
        user.recordFailedLogin(2, Duration.ofMinutes(15), NOW);
        assertTrue(user.isLocked(NOW));

        user.resetFailedLogins();

        assertFalse(user.isLocked(NOW));
        assertEquals(0, user.getFailedLogins());
        assertNull(user.getLockedUntil());
    }
}
