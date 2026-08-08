package com.finadvise.crm.users;

public record UserSearchResultDTO(
        String employeeId,
        String firstName,
        String lastName,
        String ico,
        boolean isActive
) {}
