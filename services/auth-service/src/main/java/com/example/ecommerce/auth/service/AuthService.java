package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.api.dto.AuthDtos;
import com.example.ecommerce.auth.domain.UserEntity;
import com.example.ecommerce.auth.repo.UserRepository;
import com.example.ecommerce.auth.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 業務邏輯層：
 * - 註冊：建立使用者、BCrypt hash 密碼
 * - 登入：驗證密碼，產生 token
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(AuthDtos.RegisterRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            log.warn("[register] 欄位驗證失敗：username 或 password 為空");
            throw new IllegalArgumentException("username/password 不可為空");
        }
        if (userRepository.existsByUsername(req.username())) {
            log.warn("[register] 帳號已存在 username={}", req.username());
            throw new IllegalArgumentException("帳號名稱「" + req.username() + "」已被使用，請選擇其他名稱");
        }

        String hash = passwordEncoder.encode(req.password());
        // Demo：預設註冊者都是 USER；後面可新增 admin seed
        userRepository.save(new UserEntity(req.username(), hash, "USER"));
    }

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        var user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> {
                    log.warn("[login] 帳號不存在 username={}", req.username());
                    return new IllegalArgumentException("帳號或密碼錯誤");
                });

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.warn("[login] 密碼錯誤 username={}", req.username());
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getUsername());
        return new AuthDtos.TokenResponse(access, refresh, jwtService.getAccessTtlSeconds());
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[me] 找不到使用者 username={}", username);
                    return new IllegalArgumentException("找不到使用者");
                });
        return new AuthDtos.MeResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
