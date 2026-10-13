package com.library360.member;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Twelve-hour pickup window is the code-level default for fresh policy rows. */
class LoanPolicyDefaultsTest {

    @Test
    void defaultHoldWindowIsTwelveHours() {
        assertEquals(12, new LoanPolicy().getHoldHours());
    }
}
