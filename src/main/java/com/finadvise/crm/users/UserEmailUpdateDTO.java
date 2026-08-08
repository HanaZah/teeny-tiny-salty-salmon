package com.finadvise.crm.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserEmailUpdateDTO(
        @NotNull(message = "Version is required")
        Integer version,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email format")
        String email
) {}
