package com.liquilabs.vankoo.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordTest {

    @Test
    void keepsTheGivenValue() {
        assertEquals("$2a$10$hash", new Password("$2a$10$hash").password());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsABlankValue(String value) {
        assertThrows(IllegalArgumentException.class, () -> new Password(value));
    }
}
