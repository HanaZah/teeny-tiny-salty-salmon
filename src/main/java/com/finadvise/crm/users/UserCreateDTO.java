package com.finadvise.crm.users;

import jakarta.validation.constraints.*;

public record UserCreateDTO(
        @NotBlank(message = "IČO is required")
        @Size(max = 8, min = 8, message = "IČO must be exactly 8 characters long")
        @Pattern(regexp = "^\\d{8}", message = "IČO must contain exactly 8 digits")
        String ico,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']+$", message = "First name contains invalid characters.")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']+$", message = "Last name contains invalid characters.")
        String lastName,

        @NotBlank(message = "Phone is required")
        @Size(max = 20)
        @Pattern(regexp = "^\\+?[\\d\\s\\-]+$", message = "Invalid phone format.")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email format")
        String email
) {}
