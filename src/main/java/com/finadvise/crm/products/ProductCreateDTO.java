package com.finadvise.crm.products;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductCreateDTO(
        @NotBlank(message = "product.name.required")
        @Size(max = 150, message = "product.name.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-']+$",
                message = "product.name.format"
        )
        String name,

        @NotNull(message = "product.amount.required")
        @DecimalMin(value = "0.00", message = "product.amount.min")
        @DecimalMax(value = "99999999.99", message = "product.amount.max")
        @Digits(integer = 8, fraction = 2, message = "product.amount.digits")
        BigDecimal amount,

        @NotNull(message = "product.start-date.required")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "product.type.required")
        Long productTypeId,

        @NotNull(message = "product.provider.required")
        Long productProviderId,

        @NotNull(message = "product.is-external.required")
        Boolean isExternal
) {}