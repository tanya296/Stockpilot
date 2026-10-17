package com.stockpilot.service;

import com.stockpilot.dto.AuthDtos.AuthResponse;
import com.stockpilot.dto.AuthDtos.LoginRequest;
import com.stockpilot.dto.AuthDtos.RegisterRequest;
import com.stockpilot.entity.Role;
import com.stockpilot.entity.User;
import com.stockpilot.exception.ConflictException;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @RequiredArgsConstructor generates a constructor for the final fields.
 * Spring sees the one constructor and injects the beans: this is constructor injection,
 * preferred over @Autowired fields because dependencies are final, explicit, and easy to mock in tests.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        // Self-registration is always STAFF. The client never chooses its own role;
        // only an ADMIN can promote someone.
        user.setRole(Role.STAFF);
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Same error for "no such user" and "wrong password", so attackers can't
        // find out which emails have accounts.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getEmail(), user.getFullName(), user.getRole());
    }
}
