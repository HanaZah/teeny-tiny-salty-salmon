package com.finadvise.crm.users;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdvisorSuggestionRequestDTO(
        @Size(max = 100, message = "advisor.suggestion.name.size")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']*$", message = "advisor.suggestion.name.format")
        String name,

        @Min(value = 1, message = "advisor.suggestion.limit.min")
        @Max(value = 100, message = "advisor.suggestion.limit.max")
        int limit
) {}