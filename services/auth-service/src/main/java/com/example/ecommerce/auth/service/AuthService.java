package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.api.dto.AuthDtos;
import com.example.ecommerce.auth.domain.UserEntity;
import com.example.ecommerce.auth.repo.UserRepository;
import com.example.ecommerce.auth.security.JwtService;

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
            throw new IllegalArgumentException("username/password 不可為空");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("使用者已存在");
        }

        String hash = passwordEncoder.encode(req.password());
        // Demo：預設註冊者都是 USER；後面可新增 admin seed
        userRepository.save(new UserEntity(req.username(), hash, "USER"));
    }

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        var user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getUsername());
        return new AuthDtos.TokenResponse(access, refresh, jwtService.getAccessTtlSeconds());
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
        return new AuthDtos.MeResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
