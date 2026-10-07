package com.liquilabs.vankoo.iam.infrastructure.tokens.jwt.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceImplTest {

    private static final String SECRET = "a-test-signing-secret-of-at-least-32-bytes";
    private static final String OTHER_SECRET = "another-signing-secret-of-at-least-32-bytes";

    private TokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = tokenServiceWith(SECRET, 7);
    }

    @Test
    void aGeneratedTokenIsValidAndCarriesTheUserId() {
        var token = service.generateToken("user-123", "carlos@vankoo.pe", List.of("ROLE_MYPE"));

        assertTrue(service.validateToken(token));
        assertEquals("user-123", service.getUsernameFromToken(token));
    }

    @Test
    void aGeneratedTokenCarriesEmailAndRolesClaims() {
        var token = service.generateToken("user-123", "carlos@vankoo.pe", List.of("ROLE_MYPE"));

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertEquals("carlos@vankoo.pe", claims.get("email"));
        assertEquals(List.of("ROLE_MYPE"), claims.get("roles"));
    }

    @Test
    void aTokenSignedWithAnotherKeyIsInvalid() {
        var foreignToken = tokenServiceWith(OTHER_SECRET, 7).generateToken("user-123", null, List.of());

        assertFalse(service.validateToken(foreignToken));
    }

    @Test
    void anExpiredTokenIsInvalid() {
        var expiredToken = tokenServiceWith(SECRET, -1).generateToken("user-123", null, List.of());

        assertFalse(service.validateToken(expiredToken));
    }

    @Test
    void aMalformedTokenIsInvalid() {
        assertFalse(service.validateToken("not-a-jwt"));
    }

    @Test
    void extractsTheBearerTokenFromTheAuthorizationHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def.ghi");

        assertEquals("abc.def.ghi", service.getBearerTokenFrom(request));
    }

    @Test
    void returnsNullWhenTheHeaderIsMissingOrNotBearer() {
        var withoutHeader = new MockHttpServletRequest();
        var basicAuth = new MockHttpServletRequest();
        basicAuth.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        assertNull(service.getBearerTokenFrom(withoutHeader));
        assertNull(service.getBearerTokenFrom(basicAuth));
    }

    private static TokenServiceImpl tokenServiceWith(String secret, int expirationDays) {
        var tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", secret);
        ReflectionTestUtils.setField(tokenService, "expirationDays", expirationDays);
        return tokenService;
    }
}
