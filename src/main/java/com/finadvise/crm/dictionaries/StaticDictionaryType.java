package com.finadvise.crm.dictionaries;

import com.finadvise.crm.common.InvalidInputValueException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum StaticDictionaryType {
    CLIENT_STATUS("client-status"),
    PRODUCT_STATUS("product-status"),
    USER_STATUS("user-status"),
    USER_TYPE("user-type"),
    DYNAMIC_DICTIONARIES("dynamic-dictionaries");

    private final String pathValue;

    private static final Map<String, StaticDictionaryType> LOOKUP_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(e -> e.pathValue, e -> e));

    public static StaticDictionaryType fromPathValue(String pathValue) {
        StaticDictionaryType type = LOOKUP_MAP.get(pathValue);
        if (type == null) {
            throw new InvalidInputValueException("error.dictionary.static.unknown");
        }
        return type;
    }
}