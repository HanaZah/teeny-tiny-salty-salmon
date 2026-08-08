package com.finadvise.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crm.security")
public record SecurityProperties(
        String self,
        String jwtSecret,
        String hashidSalt,
        Integer hashidLength,
        String hashidAlphabet
) {}
