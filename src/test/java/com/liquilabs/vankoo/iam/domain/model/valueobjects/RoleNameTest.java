package com.liquilabs.vankoo.iam.domain.model.valueobjects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_MYPE", "ROLE_INVESTOR"})
    void resolvesTheRolesAVisitorMayAskFor(String name) {
        assertEquals(Optional.of(RoleName.valueOf(name)), RoleName.selfAssignableFrom(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_USER"})
    void refusesPrivilegedOrInternalRoles(String name) {
        assertTrue(RoleName.selfAssignableFrom(name).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "role_mype", "ROLE_ROOT", "MYPE"})
    void refusesNamesThatAreNotAnExactRole(String name) {
        assertTrue(RoleName.selfAssignableFrom(name).isEmpty());
    }
}
