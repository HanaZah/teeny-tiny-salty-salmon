package com.finadvise.crm.users;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSearchCriteriaDTO(
        @Size(max = 100, message = "Search query is too long")
        String name,

        @Size(max = 8, message = "IČO must be at most 8 digits long")
        @Pattern(regexp = "\\d{0,8}", message = "IČO consist of at most 8 digits")
        String ico,
        UserStatus status
) {}
