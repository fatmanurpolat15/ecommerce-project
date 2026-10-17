package com.fatmanur.ecommerce.auth.dto;

import com.fatmanur.ecommerce.user.enums.Role;

public record LoginResponse(
        Long id,
        String token,
        String email,
        String name,
        Role role
) {}
