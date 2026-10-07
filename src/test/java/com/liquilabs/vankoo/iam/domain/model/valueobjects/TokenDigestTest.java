package com.liquilabs.vankoo.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenDigestTest {

    @Test
    void equalDigestsAreEqualValues() {
        assertEquals(new TokenDigest("ab12"), new TokenDigest("ab12"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectsABlankDigest(String digest) {
        assertThrows(IllegalArgumentException.class, () -> new TokenDigest(digest));
    }
}
