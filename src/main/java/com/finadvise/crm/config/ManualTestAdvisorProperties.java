package com.finadvise.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( prefix = "crm.manual-test-user.initial")
public record ManualTestAdvisorProperties(
        String employeeId,
        String password,
        String email,
        String phone,
        String firstName,
        String lastName
) {}