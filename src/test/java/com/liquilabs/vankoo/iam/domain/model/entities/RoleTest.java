package com.liquilabs.vankoo.iam.domain.model.entities;

import com.liquilabs.vankoo.iam.domain.exceptions.RoleNotAllowedException;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleTest {

    @Test
    void aNewRoleGetsAnIdentifierAndIsNew() {
        var role = new Role(RoleName.ROLE_MYPE);

        assertNotNull(role.getId());
        assertTrue(role.isNew());
        assertEquals("ROLE_MYPE", role.getStringName());
    }

    @Test
    void theDefaultRoleIsUser() {
        assertEquals(RoleName.ROLE_USER, Role.getDefaultRole().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_MYPE", "ROLE_INVESTOR"})
    void buildsARoleAVisitorMayAskFor(String name) {
        assertEquals(RoleName.valueOf(name), Role.toSelfAssignableRoleFromName(name).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_USER", "NOT_A_ROLE"})
    void refusesToBuildARoleAVisitorMayNotAskFor(String name) {
        assertThrows(RoleNotAllowedException.class, () -> Role.toSelfAssignableRoleFromName(name));
    }

    @Test
    void anEmptyRoleSetFallsBackToTheDefaultRole() {
        var roles = Role.validateRoleSet(List.of());

        assertEquals(1, roles.size());
        assertEquals(RoleName.ROLE_USER, roles.getFirst().getName());
    }

    @Test
    void aNullRoleSetFallsBackToTheDefaultRole() {
        assertEquals(RoleName.ROLE_USER, Role.validateRoleSet(null).getFirst().getName());
    }

    @Test
    void aNonEmptyRoleSetIsKeptAsIs() {
        var roles = List.of(new Role(RoleName.ROLE_INVESTOR));

        assertSame(roles, Role.validateRoleSet(roles));
    }
}
