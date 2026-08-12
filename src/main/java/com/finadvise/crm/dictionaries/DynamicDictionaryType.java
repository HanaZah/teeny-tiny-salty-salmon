package com.finadvise.crm.dictionaries;

import com.finadvise.crm.common.InvalidInputValueException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum DynamicDictionaryType {
    INCOME_TYPE("Druh příjmu", "income-type"),
    EXPENSE_TYPE("Druh výdaje", "expense-type"),
    PRODUCT_TYPE("Druh produktu", "product-type"),
    PRODUCT_PROVIDER("Poskytovatel produktu", "product-provider"),;

    private final String label;
    private final String pathValue;

    // Cached map for O(1) lookups
    private static final Map<String, DynamicDictionaryType> LOOKUP_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(e -> e.pathValue, e -> e));

    public static DynamicDictionaryType fromPathValue(String pathValue) {
        DynamicDictionaryType type = LOOKUP_MAP.get(pathValue);
        if (type == null) {
            throw new InvalidInputValueException("Unknown dynamic dictionary type: " + pathValue);
        }
        return type;
    }
}
