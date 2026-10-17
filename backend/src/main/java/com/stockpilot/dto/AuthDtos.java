package com.stockpilot.dto;

import com.stockpilot.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs (Data Transfer Objects) define exactly what the API accepts and returns.
 * We never expose entities directly: User has passwordHash, which must never leave the server.
 * Records are immutable and give us constructor, getters, equals/hashCode and toString for free.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
            @NotBlank String fullName
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String token,
            String email,
            String fullName,
            Role role
    ) {
    }
}
