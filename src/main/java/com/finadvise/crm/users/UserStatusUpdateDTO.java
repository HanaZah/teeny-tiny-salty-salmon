package com.finadvise.crm.users;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateDTO(
        @NotNull(message = "user.status.required")
        Boolean isActive
) {}