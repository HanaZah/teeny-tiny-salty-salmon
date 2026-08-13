package com.finadvise.crm.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "login.employee-id.required")
        @Size(max = 20, message = "login.employee-id.size")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "login.employee-id.format"
        )
        String employeeId,

        @NotBlank(message = "login.password.required")
        @Size(min = 8, max = 72, message = "login.password.size")
        String password
) {}