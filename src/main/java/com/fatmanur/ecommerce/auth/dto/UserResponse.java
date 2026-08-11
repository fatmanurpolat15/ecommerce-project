package com.fatmanur.ecommerce.auth.dto;

import com.fatmanur.ecommerce.user.enums.Role;

public record UserResponse(
        Long id,
        String email,
        String name,
        Role role
) {}
