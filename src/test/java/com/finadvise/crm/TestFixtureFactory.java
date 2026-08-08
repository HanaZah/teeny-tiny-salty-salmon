package com.finadvise.crm;

import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;

public class TestFixtureFactory {
    public static User createValidUser(String employeeId, UserType userType) {
        return User.builder()
                .id(1L)
                .ico("00000001")
                .employeeId(employeeId)
                .passwordHash("$2a$10$dXJ3ADWBr8t9BqbaEcKXvO7fH7Fm7ZtZ7yq7x7y7x7y7x7y7x7y7x") // Mock BCrypt string
                .firstName("John")
                .lastName("Doe")
                .email(employeeId + "@finadvise.com")
                .phone("+420123456789")
                .userType(userType)
                .version(0)
                .isActive(true)
                .build();
    }

    public static User createIntegrationUser(
            Long id, String employeeId, String encodedPassword, UserType userType, String ico) {
        return User.builder()
                .id(id)
                .ico(ico)
                .employeeId(employeeId)
                .passwordHash(encodedPassword)
                .firstName("Integration")
                .lastName("Test")
                .email(employeeId + "@finadvise.com")
                .phone("+420111222333")
                .userType(userType)
                .isActive(true)
                .build();
    }

    public static User createIntegrationAdmin(Long id, String employeeId, String encodedPassword) {
        return createIntegrationUser(id, employeeId, encodedPassword, UserType.ADMIN, null);
    }
}