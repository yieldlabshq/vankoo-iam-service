package com.liquilabs.vankoo.iam.infrastructure.tokens.secret.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretTokenServiceImplTest {

    private final SecretTokenServiceImpl service = new SecretTokenServiceImpl();

    @Test
    void generatesAUrlSafeSecretOf32RandomBytes() {
        var secret = service.generateSecret();

        // 32 bytes in unpadded Base64 are 43 characters.
        assertEquals(43, secret.length());
        assertTrue(secret.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void generatesADifferentSecretEachTime() {
        assertNotEquals(service.generateSecret(), service.generateSecret());
    }

    @Test
    void digestIsTheHexSha256OfTheSecret() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                service.digestOf("abc"));
    }

    @Test
    void digestIsDeterministic() {
        var secret = service.generateSecret();

        assertEquals(service.digestOf(secret), service.digestOf(secret));
    }
}
