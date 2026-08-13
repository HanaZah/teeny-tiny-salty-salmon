package com.finadvise.crm.clients;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientSuggestionRequestDTO(
        @Size(max = 100, message = "client.suggestion.name.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']*$", message = "client.suggestion.name.format")
        String name,

        @Min(value = 1, message = "client.suggestion.limit.min")
        @Max(value = 100, message = "client.suggestion.limit.max")
        int limit
) {}