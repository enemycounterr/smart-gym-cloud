package com.sprint.auth.service;

import com.sprint.auth.model.Role;
import com.sprint.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-super-secret-jwt-key-must-be-at-least-256-bits-long-for-hs256";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L);
    }

    @Test
    @DisplayName("generateToken: should embed username, roles, and email into JWT claims")
    void generateToken_shouldEmbedUserClaims() {
        User user = new User("danek", "encoded-pass", Role.ADMIN);
        user.setEmail("danek@smartgym.com");

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractAllClaims(token);

        assertEquals("danek", claims.getSubject());
        assertEquals("danek@smartgym.com", claims.get("email", String.class));
        assertEquals(List.of("ROLE_ADMIN"), claims.get("roles", List.class));
    }

    @Test
    @DisplayName("extractAllClaims: should throw ExpiredJwtException when token is expired")
    void extractAllClaims_whenTokenExpired_shouldThrowExpiredJwtException() {
        JwtService expiredJwtService = new JwtService(SECRET, -1000L);
        User user = new User("danek", "encoded-pass", Role.GUARD);

        String expiredToken = expiredJwtService.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractAllClaims(expiredToken));
    }

    @Test
    @DisplayName("extractAllClaims: should throw SignatureException when signed with another secret")
    void extractAllClaims_whenSignedWithWrongKey_shouldThrowSignatureException() {
        JwtService otherKeyService = new JwtService(
                "another-completely-different-secret-key-at-least-256-bits-long",
                900_000L
        );
        User user = new User("danek", "encoded-pass", Role.ADMIN);
        String forgedToken = otherKeyService.generateToken(user);

        assertThrows(SignatureException.class, () -> jwtService.extractAllClaims(forgedToken));
    }


}