package com.finadvise.crm.users;

public record UserProfileDTO(
        Integer version,
        String employeeId,
        UserType userType,
        String firstName,
        String lastName,
        String ico,
        String email,
        String phone,
        AdvisorStatisticsDTO advisorStatistics // optional, only for advisors
) {}
