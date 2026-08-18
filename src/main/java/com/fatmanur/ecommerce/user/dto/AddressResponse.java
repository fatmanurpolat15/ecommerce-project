package com.fatmanur.ecommerce.user.dto;

import java.time.LocalDateTime;

public record AddressResponse(
    Long id,
    String address,
    String street,
    String district,
    String city,
    String country,
    String zipCode,
    boolean isDefault
) {}
