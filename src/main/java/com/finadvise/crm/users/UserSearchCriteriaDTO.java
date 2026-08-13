package com.finadvise.crm.users;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSearchCriteriaDTO(
        @Size(max = 100, message = "user.search.name.size")
        String name,

        @Size(max = 8, message = "user.search.ico.size")
        @Pattern(regexp = "\\d{0,8}", message = "user.search.ico.format")
        String ico,
        UserStatus status
) {}