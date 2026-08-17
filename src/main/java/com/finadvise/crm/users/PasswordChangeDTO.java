package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeDTO(
        @NotBlank(message = "user.password.required")
        @Size(min = 8, max = 255, message = "user.password.size")
        String currentPassword,

        @NotBlank(message = "user.password.required")
        @Size(min = 8, max = 255, message = "user.password.size")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\d\\p{P}\\p{S} ]+$",
                message = "user.password.format"
        )
        String newPassword
) {}