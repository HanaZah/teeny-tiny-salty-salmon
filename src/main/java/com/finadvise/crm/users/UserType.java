package com.finadvise.crm.users;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    ADMIN("Admin"),
    ADVISOR("Poradce");

    private final String label;
}
