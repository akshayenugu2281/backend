package com.library360.catalog;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for backend-generated unique accession numbers. */
class AccessionNumbersTest {

    @Test
    void formatIsDeterministicAndZeroPadded() {
        assertEquals("ACC-2026-000007", AccessionNumbers.format(2026, 7));
        assertEquals("ACC-2026-123456", AccessionNumbers.format(2026, 123456));
    }

    @Test
    void temporaryValuesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String tmp = AccessionNumbers.temporary();
            assertTrue(tmp.startsWith("TMP-"), "temporary accession must be recognizable");
            assertTrue(seen.add(tmp), "temporary accession collision");
        }
    }

    @Test
    void yearPrefersAcquiredDateThenFallback() {
        assertEquals(2024, AccessionNumbers.yearOf(LocalDate.of(2024, 3, 1), LocalDate.of(2026, 1, 1)));
        assertEquals(2026, AccessionNumbers.yearOf(null, LocalDate.of(2026, 1, 1)));
    }
}
