package com.finadvise.crm.clients;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum ClientStatus {
    ACTIVE("Aktivní"),
    INACTIVE("Neaktivní"),
    ALL("Všichni");

    private final String label;
}