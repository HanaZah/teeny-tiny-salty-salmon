package com.finadvise.crm.users;

import jakarta.validation.constraints.*;

public record UserCreateDTO(
        @NotBlank(message = "user.ico.required")
        @Size(max = 8, min = 8, message = "user.ico.size")
        @Pattern(regexp = "^\\d{8}", message = "user.ico.format")
        String ico,

        @NotBlank(message = "user.first-name.required")
        @Size(max = 50, message = "user.first-name.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']+$", message = "user.first-name.format")
        String firstName,

        @NotBlank(message = "user.last-name.required")
        @Size(max = 50, message = "user.last-name.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']+$", message = "user.last-name.format")
        String lastName,

        @NotBlank(message = "user.phone.required")
        @Size(max = 20, message = "user.phone.size")
        @Pattern(regexp = "^\\+?[\\d\\s\\-]+$", message = "user.phone.format")
        String phone,

        @NotBlank(message = "user.email.required")
        @Email(message = "user.email.format")
        String email
) {}