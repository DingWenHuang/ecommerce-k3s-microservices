package com.example.ecommerce.auth.api;

import com.example.ecommerce.auth.api.dto.AuthDtos;
import com.example.ecommerce.auth.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * auth API：
 * - register：註冊
 * - login：登入回 token
 * - me：驗證 token 是否有效（示範保護資源）
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDtos.RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.TokenResponse> login(@RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.MeResponse> me(Authentication authentication) {
        // authentication.getName() 會是 JwtAuthFilter 放入的 username
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }
}