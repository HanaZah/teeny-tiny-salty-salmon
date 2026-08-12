package com.finadvise.crm.users;

import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;
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

    AdvisorSummaryDTO toAdvisorSummaryDto(User user) {
        if (user == null) {
            return null;
        }

        return new AdvisorSummaryDTO(
                user.getEmployeeId(),
                user.getFirstName(),
                user.getLastName());
    }

    StaticDictionaryItemDTO toStaticDictionaryItemDto(UserType type) {
        if (type == null) {
            return null;
        }
        return new StaticDictionaryItemDTO(type.name(), type.getLabel());
    }

    StaticDictionaryItemDTO toStaticDictionaryItemDto(UserStatus status) {
        if (status == null) {
            return null;
        }
        return new StaticDictionaryItemDTO(status.name(), status.getLabel());
    }
}
