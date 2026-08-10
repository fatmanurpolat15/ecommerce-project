package com.fatmanur.ecommerce.auth.controller;

import com.fatmanur.ecommerce.auth.dto.LoginRequest;
import com.fatmanur.ecommerce.auth.dto.LoginResponse;
import com.fatmanur.ecommerce.auth.dto.MeResponse;
import com.fatmanur.ecommerce.auth.dto.RegisterRequest;
import com.fatmanur.ecommerce.auth.exception.UserNotFoundException;
import com.fatmanur.ecommerce.auth.service.AuthService;
import com.fatmanur.ecommerce.user.entity.User;
import com.fatmanur.ecommerce.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return ResponseEntity.ok(new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()));
    }
}
