package com.fraud.api;

import com.fraud.api.security.JwtService;
import com.fraud.api.security.RefreshTokenService;
import com.fraud.api.security.TokenBlacklist;
import com.fraud.application.audit.AuditTrail;
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
 * Authentication endpoints: login, token refresh, logout.
 *
 * Credentials are verified against the persistent user store (users table, BCrypt hashes).
 * Login returns a SHORT-LIVED access JWT (roles claim, RBAC) plus a rotating server-side refresh
 * token; /refresh exchanges the refresh token for a fresh pair (one-shot rotation with reuse
 * detection — see RefreshTokenService). All outcomes are written to the persistent audit trail.
 * The endpoints are protected against brute-force by LoginRateLimitFilter.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final UserAccountJpaRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final AuditTrail audit;
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final String unknownUserHash;

    public AuthController(JwtService jwtService,
                          TokenBlacklist blacklist,
                          UserAccountJpaRepository users,
                          PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokens,
                          AuditTrail audit,
                          MeterRegistry meterRegistry) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.loginSuccess = meterRegistry.counter("fraud.auth.login", "result", "success");
        this.loginFailure = meterRegistry.counter("fraud.auth.login", "result", "failure");
        // TIMING/ENUMERATION DEFENSE: a real BCrypt hash that matches nothing. When the username
        // does not exist we still run a full-cost matches() against this, so "unknown user" and
        // "wrong password" take the same time — usernames cannot be enumerated via response timing.
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record TokenResponse(String token, String refreshToken) {}

    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByUsername(request.username()).orElse(null);
        String hash = (user != null && user.isEnabled()) ? user.getPasswordHash() : unknownUserHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (user == null || !user.isEnabled() || !passwordOk) {
            loginFailure.increment();
            audit.record("LOGIN_FAILURE", "username=" + request.username());
            // Same message for every failure mode — the response must not reveal whether the
            // username exists or the account is disabled.
            return ApiResult.fail("Invalid username or password");
        }
        loginSuccess.increment();
        audit.record("LOGIN_SUCCESS", "username=" + user.getUsername());
        return ApiResult.ok(issuePair(user));
    }

    /**
     * Exchanges a refresh token for a new access+refresh pair (rotation: the presented token is
     * consumed; its successor is returned). An unknown/expired/reused token gets one generic
     * message — and reuse additionally revokes the user's whole refresh-token family
     * (see RefreshTokenService.consume).
     */
    @PostMapping("/refresh")
    public ApiResult<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        UserAccount user = refreshTokens.consume(request.refreshToken())
                .flatMap(users::findByUsername)
                .filter(UserAccount::isEnabled)
                .orElse(null);
        if (user == null) {
            audit.record("TOKEN_REFRESH_REJECTED", "invalid, expired or reused refresh token");
            return ApiResult.fail("Invalid or expired refresh token");
        }
        audit.record("TOKEN_REFRESHED", "username=" + user.getUsername());
        return ApiResult.ok(issuePair(user));
    }

    /**
     * Logout: blacklists the access token's jti AND revokes all of the user's refresh tokens —
     * without the latter, "logout" would be cosmetic (a stored refresh token could mint fresh
     * access tokens a second later).
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            Claims claims = jwtService.validate(authHeader.substring(7));
            if (claims != null) {
                blacklist.revoke(claims.getId());
                refreshTokens.revokeAll(claims.getSubject());
                audit.record("LOGOUT", "username=" + claims.getSubject());
            }
        }
        return ApiResult.ok(null, "Logged out");
    }

    private TokenResponse issuePair(UserAccount user) {
        return new TokenResponse(
                jwtService.issue(user.getUsername(), user.roleSet()),
                refreshTokens.issue(user.getUsername()));
    }
}
