package com.liquilabs.vankoo.iam.domain.model.aggregates;

import com.liquilabs.vankoo.iam.domain.model.entities.Role;
import com.liquilabs.vankoo.iam.domain.model.events.PasswordChangedEvent;
import com.liquilabs.vankoo.iam.domain.model.events.UserCreatedEvent;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Password;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final Email EMAIL = new Email("carlos@vankoo.pe");
    private static final Password HASHED_PASSWORD = new Password("$2a$10$hashed");

    @Test
    void aNewUserHasAnIdentityAndNoRolesYet() {
        var user = new User(EMAIL, HASHED_PASSWORD);

        assertNotNull(user.getId());
        assertTrue(user.getRoles().isEmpty());
        assertTrue(user.isNew());
    }

    @Test
    void aUserCreatedWithoutRolesGetsTheDefaultRole() {
        var user = new User(EMAIL, HASHED_PASSWORD, List.of());

        assertEquals(List.of(RoleName.ROLE_USER), roleNamesOf(user));
    }

    @Test
    void aUserCreatedWithRolesKeepsThem() {
        var user = new User(EMAIL, HASHED_PASSWORD, List.of(new Role(RoleName.ROLE_MYPE)));

        assertEquals(List.of(RoleName.ROLE_MYPE), roleNamesOf(user));
    }

    @Test
    void changingThePasswordReplacesTheStoredValue() {
        var user = new User(EMAIL, HASHED_PASSWORD);
        var newPassword = new Password("$2a$10$other");

        user.changePassword(newPassword);

        assertEquals(newPassword, user.getPassword());
    }

    @Test
    void registeringCreationRecordsAUserCreatedEventWithIdEmailAndRoles() {
        var user = new User(EMAIL, HASHED_PASSWORD, List.of(new Role(RoleName.ROLE_INVESTOR)));

        user.registerUserCreatedEvent();

        var event = assertInstanceOf(UserCreatedEvent.class, singleEventOf(user));
        assertEquals(user.getId().id().toString(), event.id());
        assertEquals("carlos@vankoo.pe", event.email());
        assertEquals(List.of("ROLE_INVESTOR"), event.roles());
    }

    @Test
    void registeringAPasswordChangeRecordsAPasswordChangedEvent() {
        var user = new User(EMAIL, HASHED_PASSWORD);

        user.registerPasswordChangedEvent();

        var event = assertInstanceOf(PasswordChangedEvent.class, singleEventOf(user));
        assertEquals(user.getId().id().toString(), event.id());
        assertEquals("carlos@vankoo.pe", event.email());
    }

    private static List<RoleName> roleNamesOf(User user) {
        return user.getRoles().stream().map(Role::getName).toList();
    }

    private static Object singleEventOf(User user) {
        Collection<?> events = ReflectionTestUtils.invokeMethod(user, "domainEvents");
        assertNotNull(events);
        assertEquals(1, events.size());
        return events.iterator().next();
    }
}
