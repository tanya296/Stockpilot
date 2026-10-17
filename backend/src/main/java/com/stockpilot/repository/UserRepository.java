package com.stockpilot.repository;

import com.stockpilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Spring Data generates the implementation at startup. The SQL comes from the method name:
// findByEmail -> "select ... from users where email = ?".
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
