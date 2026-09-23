package com.fatmanur.ecommerce.auth.dto;

import com.fatmanur.ecommerce.user.enums.Role;

public record AuthSession(
        String token,
        String refreshToken,
        String email,
        String name,
        Role role
) {}