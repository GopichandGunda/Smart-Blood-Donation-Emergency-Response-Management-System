package com.bloodmanagement.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {
    @Test
    void verifiesPasswordAgainstSaltedHash() {
        char[] password = "correct horse battery".toCharArray();
        String hash = PasswordUtil.hash(password);

        assertTrue(PasswordUtil.verify(password, hash));
        assertFalse(PasswordUtil.verify("incorrect password".toCharArray(), hash));
    }

    @Test
    void rejectsWeakPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("short".toCharArray()));
    }
}
