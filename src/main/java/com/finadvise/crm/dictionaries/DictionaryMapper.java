package com.finadvise.crm.dictionaries;

import org.springframework.stereotype.Component;

@Component
class DictionaryMapper {
    StaticDictionaryItemDTO toStaticDictionaryItemDto(DynamicDictionaryType type) {
        return new StaticDictionaryItemDTO(type.getPathValue(), type.getLabel());
    }
}
