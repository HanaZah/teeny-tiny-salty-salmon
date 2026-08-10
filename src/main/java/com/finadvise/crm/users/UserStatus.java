package com.finadvise.crm.users;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
enum UserStatus {
    ACTIVE("Aktivní"),
    INACTIVE("Neaktivní"),
    ALL("Všichni");

    private final String label;
}
