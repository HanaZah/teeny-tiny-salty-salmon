package com.finadvise.crm.products;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum ProductStatus {
    ACTIVE("Aktivní"),
    EXPIRED("Expirované"),
    FUTURE("Budoucí"),
    ALL("Všechny");

    private final String label;
}
