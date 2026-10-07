package com.liquilabs.vankoo.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserIdTest {

    @Test
    void generatesTimeOrderedVersion7Identifiers() {
        assertEquals(7, new UserId().id().version());
    }

    @Test
    void generatesADifferentIdentifierEachTime() {
        assertNotEquals(new UserId(), new UserId());
    }

    @Test
    void wrapsAnExistingIdentifier() {
        var uuid = UUID.randomUUID();
        assertEquals(uuid, new UserId(uuid).id());
    }

    @Test
    void rejectsANullIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
    }
}
