package com.fatmanur.ecommerce.auth.dto;

public record LoginResponse(
        String token,
        String email,
        String role
) {}
