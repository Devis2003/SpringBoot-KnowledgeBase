package com.devi.knowledgebase.controller;

import com.devi.knowledgebase.dto.auth.AuthResponse;
import com.devi.knowledgebase.dto.auth.LoginRequest;
import com.devi.knowledgebase.dto.auth.RegisterRequest;
import com.devi.knowledgebase.dto.auth.UserResponse;
import com.devi.knowledgebase.entity.User;
import com.devi.knowledgebase.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.devi.knowledgebase.dto.auth.RefreshTokenRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }
}