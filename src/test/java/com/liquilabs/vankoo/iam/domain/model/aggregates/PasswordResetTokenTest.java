package com.liquilabs.vankoo.iam.domain.model.aggregates;

import com.liquilabs.vankoo.iam.domain.model.events.PasswordResetRequestedEvent;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.TokenDigest;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetTokenTest {

    private static final long MINUTE = 60_000L;

    private Date now;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        now = new Date();
        token = new PasswordResetToken(new UserId(), new TokenDigest("digest"), new Date(now.getTime() + 30 * MINUTE));
    }

    @Test
    void isUsableBeforeItExpires() {
        assertTrue(token.isUsable(now));
    }

    @Test
    void isNotUsableOnceItExpires() {
        assertFalse(token.isUsable(new Date(now.getTime() + 31 * MINUTE)));
    }

    @Test
    void isNotUsableExactlyAtItsExpiration() {
        assertFalse(token.isUsable(token.getExpiresAt()));
    }

    @Test
    void isNotUsableAfterBeingConsumed() {
        token.consume(now);

        assertEquals(now, token.getConsumedAt());
        assertFalse(token.isUsable(now));
    }

    @Test
    void registeringTheRequestRecordsAnEventCarryingTheSecretAndExpiration() {
        token.registerPasswordResetRequestedEvent(new Email("sofia@vankoo.pe"), "plain-secret");

        Collection<?> events = ReflectionTestUtils.invokeMethod(token, "domainEvents");
        assertNotNull(events);
        var event = assertInstanceOf(PasswordResetRequestedEvent.class, events.iterator().next());
        assertEquals("sofia@vankoo.pe", event.email());
        assertEquals("plain-secret", event.token());
        assertEquals(token.getExpiresAt(), event.expiresAt());
    }
}
