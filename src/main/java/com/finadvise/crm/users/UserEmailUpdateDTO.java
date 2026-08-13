package com.finadvise.crm.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserEmailUpdateDTO(
        @NotNull(message = "user.version.required")
        Integer version,

        @NotBlank(message = "user.email.required")
        @Email(message = "user.email.format")
        String email
) {}