package com.devi.knowledgebase.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
