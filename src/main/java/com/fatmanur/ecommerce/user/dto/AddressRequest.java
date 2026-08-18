package com.fatmanur.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @NotBlank String address,
    @NotBlank String street,
    @NotBlank String district,
    @NotBlank String city,
    @NotBlank String country,
    @NotBlank @Size(min = 5, max = 10) String zipCode
) {}
