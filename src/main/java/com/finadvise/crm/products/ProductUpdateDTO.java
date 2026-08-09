package com.finadvise.crm.products;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductUpdateDTO(
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters long")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-']+$",
                message = "Product name contains invalid characters." +
                        "Please use only standard letters, digits and basic punctuation."
        )
        String name,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Amount cannot be negative")
        @DecimalMax(value = "99999999.99", inclusive = true, message = "Amount exceeds maximum limit")
        @Digits(integer = 8, fraction = 2, message = "Amount must have up to 8 integer digits and 2 fractional digits")
        BigDecimal amount,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Product type is required")
        Long productTypeId,

        @NotNull(message = "Product provider is required")
        Long productProviderId
) {
}
