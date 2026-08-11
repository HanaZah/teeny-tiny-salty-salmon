package com.finadvise.crm.clients;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientSuggestionRequestDTO(
        @Size(max = 100, message = "Search query is too long")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']*$", message = "Search query contains invalid characters.")
        String name,

        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit cannot exceed 100")
        int limit
) {}