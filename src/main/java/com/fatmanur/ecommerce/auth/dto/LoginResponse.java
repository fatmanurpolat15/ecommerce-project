package com.fatmanur.ecommerce.auth.dto;

import com.fatmanur.ecommerce.user.enums.Role;

public record LoginResponse(
        String token,
        String email,
        Role role
) {}
