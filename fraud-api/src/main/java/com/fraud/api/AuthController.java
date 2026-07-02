package com.fraud.api;

import com.fraud.api.security.JwtService;
import com.fraud.api.security.TokenBlacklist;
import com.fraud.application.common.ApiResult;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Authentication (login/logout) endpoints — issues demo tokens.
 *
 * NOTE: User/password verification is kept simple here (a fixed demo password). In production,
 * add a user store + password hashing (BCrypt) + role/claim management.
 * The endpoint is protected against brute-force by {@link com.fraud.api.security.LoginRateLimitFilter}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final String demoPassword;

    public AuthController(JwtService jwtService,
                          TokenBlacklist blacklist,
                          @Value("${fraud.security.demo-password:fraud123}") String demoPassword) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.demoPassword = demoPassword;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String token) {}

    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        if (!constantTimeEquals(demoPassword, request.password())) {
            return ApiResult.fail("Invalid username or password");
        }
        return ApiResult.ok(new TokenResponse(jwtService.issue(request.username())));
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

    /**
     * SECURITY: String.equals exits early (stops at the first differing character) — this exposes
     * a timing attack that lets an attacker derive the password character by character from
     * response times. MessageDigest.isEqual is constant-time; the comparison duration does not
     * depend on how correct the password is.
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
