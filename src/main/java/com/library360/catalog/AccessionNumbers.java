package com.library360.catalog;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Backend-generated unique accession numbers for physical copies.
 * Format {@code ACC-<year>-<zero-padded copy id>} is deterministic and unique
 * because the copy id is unique. New copies are first saved with a random
 * temporary value (satisfying NOT NULL + UNIQUE under concurrency) and then
 * assigned their final number once the id exists — all inside one transaction.
 */
public final class AccessionNumbers {

    private AccessionNumbers() {}

    public static String format(int year, long copyId) {
        return "ACC-" + year + "-" + String.format("%06d", copyId);
    }

    public static String temporary() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(java.util.Locale.ROOT);
    }

    public static int yearOf(LocalDate acquiredOn, LocalDate fallback) {
        LocalDate base = acquiredOn != null ? acquiredOn : (fallback != null ? fallback : LocalDate.now());
        return base.getYear();
    }
}
