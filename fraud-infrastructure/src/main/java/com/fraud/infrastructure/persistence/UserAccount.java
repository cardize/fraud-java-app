package com.fraud.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persistent user account: BCrypt password hash + roles (RBAC).
 *
 * Roles are stored as a comma-separated string WITHOUT the ROLE_ prefix (e.g. "ADMIN,USER");
 * the prefix is added when building Spring authorities at token-validation time.
 * The password NEVER exists here in plain text — only the BCrypt hash is stored.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String passwordHash;
    private String roles;
    private boolean enabled;
    private Instant createdAt;

    protected UserAccount() {
        // no-arg constructor required by JPA
    }

    public UserAccount(String username, String passwordHash, List<String> roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = String.join(",", roles);
        this.enabled = true;
        this.createdAt = Instant.now();
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }

    /** Parses the CSV roles column into a set (e.g. "ADMIN,USER" -> {ADMIN, USER}). */
    public Set<String> roleSet() {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
