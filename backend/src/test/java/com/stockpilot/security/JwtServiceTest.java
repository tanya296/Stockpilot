package com.stockpilot.security;

import com.stockpilot.entity.Role;
import com.stockpilot.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("unit-test-secret-unit-test-secret-unit-test-secret", 60);

    private User user() {
        User u = new User();
        u.setEmail("manager@test.dev");
        u.setFullName("Test Manager");
        u.setRole(Role.MANAGER);
        return u;
    }

    @Test
    void validTokenRoundTrips() {
        String token = jwtService.generateToken(user());

        var claims = jwtService.parse(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("manager@test.dev");
        assertThat(claims.get().get("role", String.class)).isEqualTo("MANAGER");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user());
        // Flip one character in the signature (the part after the last dot).
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService attacker = new JwtService("attacker-secret-attacker-secret-attacker-secret", 60);
        String forged = attacker.generateToken(user());

        assertThat(jwtService.parse(forged)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }
}
