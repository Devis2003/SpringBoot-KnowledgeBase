package com.devi.knowledgebase.dto.auth;

public record UserResponse(
        Long id,
        String email,
        String role
) {
}
