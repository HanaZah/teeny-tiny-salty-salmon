package com.finadvise.crm.dictionaries;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class StaticDictionaryTypeConverter implements Converter<String, StaticDictionaryType> {
    @Override
    public StaticDictionaryType convert(String source) {
        return StaticDictionaryType.fromPathValue(source);
    }
}
