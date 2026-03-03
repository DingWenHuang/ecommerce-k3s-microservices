package com.example.ecommerce.auth.api;

import com.example.ecommerce.auth.api.dto.AuthDtos;
import com.example.ecommerce.auth.service.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * auth API：
 * - register：註冊
 * - login：登入回 token
 * - me：驗證 token 是否有效（示範保護資源）
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDtos.RegisterRequest req) {
        authService.register(req);
        log.info("[register] 註冊成功 username={}", req.username());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.TokenResponse> login(@RequestBody AuthDtos.LoginRequest req) {
        AuthDtos.TokenResponse token = authService.login(req);
        log.info("[login] 登入成功 username={}", req.username());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.MeResponse> me(Authentication authentication) {
        log.debug("[me] 查詢使用者資訊 username={}", authentication.getName());
        // authentication.getName() 會是 JwtAuthFilter 放入的 username
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[auth] 請求失敗 message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }
}