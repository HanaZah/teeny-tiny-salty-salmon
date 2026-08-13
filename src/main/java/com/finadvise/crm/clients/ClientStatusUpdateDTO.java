package com.finadvise.crm.clients;

import jakarta.validation.constraints.NotNull;

public record ClientStatusUpdateDTO(
        // version is intentionally omitted
        // status change is a high-priority operation and must not be obstructed by race conditions
        @NotNull(message = "client.status.required")
        Boolean isActive
) {}