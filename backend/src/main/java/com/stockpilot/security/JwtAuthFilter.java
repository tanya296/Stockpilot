package com.stockpilot.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once on every request, before the controller.
 * 1. Read "Authorization: Bearer <token>".
 * 2. Verify signature + expiry.
 * 3. If valid, put an Authentication (email + ROLE_x) into the SecurityContext.
 * If there's no token or it's invalid we just continue WITHOUT authentication;
 * Spring Security then returns 401 for any endpoint that requires login.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            jwtService.parse(token).ifPresent(this::authenticate);
        }

        chain.doFilter(request, response); // always pass the request down the chain
    }

    private void authenticate(Claims claims) {
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        // Spring's hasRole('ADMIN') checks for an authority named "ROLE_ADMIN".
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
