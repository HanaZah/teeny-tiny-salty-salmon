package com.finadvise.crm.dictionaries;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class DynamicDictionaryTypeConverter implements Converter<String, DynamicDictionaryType> {
    @Override
    public DynamicDictionaryType convert(String s) {
        return DynamicDictionaryType.fromPathValue(s);
    }
}
