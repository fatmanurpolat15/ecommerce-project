package com.fatmanur.ecommerce.category.dto;

import java.io.Serializable;

public record CategoryResponse(
        Long id,
        String name,
        String description

)  implements Serializable{}



