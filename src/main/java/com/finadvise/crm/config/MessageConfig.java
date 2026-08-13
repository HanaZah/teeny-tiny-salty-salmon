package com.finadvise.crm.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("errors");
        messageSource.setDefaultEncoding("UTF-8");

        // Prevents the application from falling back to the OS-level default locale
        messageSource.setFallbackToSystemLocale(false);

        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        // Locks the application strictly to Czech, ignoring all HTTP Accept-Language headers
        return new FixedLocaleResolver(Locale.of("cs", "CZ"));
    }
}