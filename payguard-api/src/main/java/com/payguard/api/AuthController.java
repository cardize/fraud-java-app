package com.payguard.api;

import com.payguard.api.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kimlik doğrulama (login) uç noktası — demo amaçlı token üretir.
 *
 * NOT: Kullanıcı/şifre doğrulaması burada basit tutuldu (sabit demo şifresi). Üretimde
 * kullanıcı deposu + şifre hash (BCrypt) + rol/claim yönetimi eklenir.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final String demoPassword;

    public AuthController(JwtService jwtService,
                          @Value("${payguard.security.demo-password:payguard123}") String demoPassword) {
        this.jwtService = jwtService;
        this.demoPassword = demoPassword;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String token) {}

    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        if (request.username() == null || !demoPassword.equals(request.password())) {
            return ApiResult.fail("Geçersiz kullanıcı adı veya şifre");
        }
        return ApiResult.ok(new TokenResponse(jwtService.issue(request.username())));
    }
}
