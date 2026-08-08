package com.finadvise.crm.users;

public record UserContactDTO(
        String firstName,
        String lastName,
        String email,
        String phone
) {}
