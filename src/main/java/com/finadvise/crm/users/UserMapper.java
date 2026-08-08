package com.finadvise.crm.users;

import org.springframework.stereotype.Component;

@Component
class UserMapper {
    UserDetailDTO toDetailDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDetailDTO(
                user.getVersion(),
                user.getEmployeeId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getUserType(),
                user.isActive()
        );
    }

    UserContactDTO toContactDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserContactDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone()
        );
    }

    UserProfileDTO toProfileDto(User user, AdvisorStatisticsDTO advisorStatistics) {
        if (user == null) {
            return null;
        }

        return new UserProfileDTO(
                user.getVersion(),
                user.getEmployeeId(),
                user.getUserType(),
                user.getFirstName(),
                user.getLastName(),
                user.getIco(),
                user.getEmail(),
                user.getPhone(),
                advisorStatistics
        );
    }

    UserSearchResultDTO toSearchResultDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserSearchResultDTO(
                user.getEmployeeId(),
                user.getFirstName(),
                user.getLastName(),
                user.getIco(),
                user.isActive()
        );
    }
}
