package com.finadvise.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crm.admin.initial")
public record AdminProperties(
        String password,
        String email,
        String phone,
        String firstName,
        String lastName
) {}