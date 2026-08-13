package com.finadvise.crm.addresses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressInputDTO(
        @NotBlank(message = "address.street.required")
        @Size(max = 100, message = "address.street.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$",
                message = "address.street.format"
        )
        String street,

        @NotBlank(message = "address.house-number.required")
        @Size(max = 10, message = "address.house-number.size")
        @Pattern(
                regexp = "^[1-9]\\d{0,3}(/[1-9]\\d{0,3}[a-z]?)?$",
                message = "address.house-number.format"
        )
        String houseNumber,

        @NotBlank(message = "address.city.required")
        @Size(max = 100, message = "address.city.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\s\\-'.]+$",
                message = "address.city.format"
        )
        String city,

        @NotBlank(message = "address.postal-code.required")
        @Pattern(regexp = "^\\d{3}\\s\\d{2}$", message = "address.postal-code.format")
        String postalCode
) {}