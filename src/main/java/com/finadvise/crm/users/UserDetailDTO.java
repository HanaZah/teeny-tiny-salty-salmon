package com.finadvise.crm.users;

public record UserDetailDTO(
        Integer version,
        String employeeId,
        String firstName,
        String lastName,
        String phone,
        String email,
        UserType userType,
        Boolean isActive
) {}
