package com.liquilabs.vankoo.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void acceptsAWellFormedAddress() {
        assertEquals("carlos.mype@vankoo.pe", new Email("carlos.mype@vankoo.pe").email());
    }

    @Test
    void equalAddressesAreEqualValues() {
        assertEquals(new Email("sofia@vankoo.pe"), new Email("sofia@vankoo.pe"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsAMissingAddress(String address) {
        assertThrows(IllegalArgumentException.class, () -> new Email(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {"carlos", "carlos@", "@vankoo.pe", "carlos@vankoo", "carlos vankoo@mail.pe"})
    void rejectsAMalformedAddress(String address) {
        assertThrows(IllegalArgumentException.class, () -> new Email(address));
    }
}
