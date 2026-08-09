package com.fatmanur.ecommerce.auth.controller;
import com.fatmanur.ecommerce.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.fatmanur.ecommerce.auth.service.AuthService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) { //RequestBody=It converts the incoming JSON into a RegisterRequest object.
                                                           //Valid=For the @notblank and @email rules
                authService.register(request);
                return ResponseEntity.ok("User registered successfully");
    }

    public AuthController(AuthService authService){
        this.authService=authService;
    }
}
