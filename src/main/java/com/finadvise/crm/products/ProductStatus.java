package com.finadvise.crm.products;

import lombok.Getter;

@Getter
enum ProductStatus {
    ACTIVE("Aktivní"),
    EXPIRED("Expirované"),
    FUTURE("Budoucí"),
    ALL("Všechny");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }
}
