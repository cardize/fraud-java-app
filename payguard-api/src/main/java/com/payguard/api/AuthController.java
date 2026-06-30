package com.payguard.api;

import com.payguard.api.security.JwtService;
import com.payguard.api.security.TokenBlacklist;
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
 * Kimlik doğrulama (login/logout) uç noktaları — demo amaçlı token üretir.
 *
 * NOT: Kullanıcı/şifre doğrulaması burada basit tutuldu (sabit demo şifresi). Üretimde
 * kullanıcı deposu + şifre hash (BCrypt) + rol/claim yönetimi eklenir.
 * Brute-force'a karşı uç nokta {@link com.payguard.api.security.LoginRateLimitFilter} ile sınırlıdır.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final String demoPassword;

    public AuthController(JwtService jwtService,
                          TokenBlacklist blacklist,
                          @Value("${payguard.security.demo-password:payguard123}") String demoPassword) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.demoPassword = demoPassword;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String token) {}

    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        if (!constantTimeEquals(demoPassword, request.password())) {
            return ApiResult.fail("Geçersiz kullanıcı adı veya şifre");
        }
        return ApiResult.ok(new TokenResponse(jwtService.issue(request.username())));
    }

    /**
     * Stateless JWT'de gerçek bir "logout" doğal olarak yoktur (token süresi dolana kadar geçerlidir).
     * Burada token'ın jti'si kara listeye alınır; bundan sonraki istekler bu token ile reddedilir
     * (bkz. {@link com.payguard.api.security.JwtAuthenticationFilter}).
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            Claims claims = jwtService.validate(authHeader.substring(7));
            if (claims != null) {
                blacklist.revoke(claims.getId());
            }
        }
        return ApiResult.ok(null, "Çıkış yapıldı");
    }

    /**
     * GÜVENLİK: String.equals erken çıkış yapar (ilk farklı karakterde durur) — bu, yanıt süresinden
     * şifreyi karakter karakter çıkarmaya izin veren bir timing attack'a açıktır. MessageDigest.isEqual
     * sabit zamanlıdır; karşılaştırma süresi şifrenin doğruluğuna bağlı olarak değişmez.
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
