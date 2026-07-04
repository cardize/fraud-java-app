package com.fraud.api;

import com.fraud.api.security.JwtService;
import com.fraud.api.security.TokenBlacklist;
import com.fraud.application.common.ApiResult;
import com.fraud.infrastructure.persistence.UserAccount;
import com.fraud.infrastructure.persistence.UserAccountJpaRepository;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authentication (login/logout) endpoints.
 *
 * Credentials are verified against the persistent user store (users table, BCrypt hashes) and the
 * issued JWT carries the user's roles as a claim (RBAC — see JwtAuthenticationFilter/SecurityConfig).
 * The endpoint is protected against brute-force by {@link com.fraud.api.security.LoginRateLimitFilter}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final UserAccountJpaRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final String unknownUserHash;

    public AuthController(JwtService jwtService,
                          TokenBlacklist blacklist,
                          UserAccountJpaRepository users,
                          PasswordEncoder passwordEncoder,
                          MeterRegistry meterRegistry) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.loginSuccess = meterRegistry.counter("fraud.auth.login", "result", "success");
        this.loginFailure = meterRegistry.counter("fraud.auth.login", "result", "failure");
        // TIMING/ENUMERATION DEFENSE: a real BCrypt hash that matches nothing. When the username
        // does not exist we still run a full-cost matches() against this, so "unknown user" and
        // "wrong password" take the same time — usernames cannot be enumerated via response timing.
        // (BCrypt itself is deliberately slow and runs its full cost regardless of how wrong the
        // password is, which also covers the old String.equals timing-attack concern.)
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String token) {}

    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByUsername(request.username()).orElse(null);
        String hash = (user != null && user.isEnabled()) ? user.getPasswordHash() : unknownUserHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (user == null || !user.isEnabled() || !passwordOk) {
            loginFailure.increment();
            // Same message for every failure mode — the response must not reveal whether the
            // username exists or the account is disabled.
            return ApiResult.fail("Invalid username or password");
        }
        loginSuccess.increment();
        return ApiResult.ok(new TokenResponse(jwtService.issue(user.getUsername(), user.roleSet())));
    }

    /**
     * Stateless JWT has no natural "logout" (a token stays valid until it expires).
     * Here the token's jti is blacklisted; subsequent requests with this token are rejected
     * (see {@link com.fraud.api.security.JwtAuthenticationFilter}).
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            Claims claims = jwtService.validate(authHeader.substring(7));
            if (claims != null) {
                blacklist.revoke(claims.getId());
            }
        }
        return ApiResult.ok(null, "Logged out");
    }
}
