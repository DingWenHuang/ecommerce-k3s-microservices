package com.example.ecommerce.auth.api.dto;

/**
 * 為了簡化示範，把 DTO 放同一檔案
 * 實務可拆分多個檔案
 */
public class AuthDtos {

    public record RegisterRequest(String username, String password) {}
    public record LoginRequest(String username, String password) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds
    ) {}

    public record MeResponse(Long userId, String username, String role) {}
}
