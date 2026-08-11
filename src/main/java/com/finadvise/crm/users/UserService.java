package com.finadvise.crm.users;

import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.common.SystemIntegrityException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class UserService implements UserReadFacade{
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Optional<User> findByEmployeeId(String employeeId) {
        return userRepository.findByEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public UserContactDTO getAdminContact() {
       User admin = userRepository.findFirstActiveByUserType_Admin().orElseThrow(
               () -> new SystemIntegrityException("Critical system failure: No admin user found")
       );

       return userMapper.toContactDto(admin);
    }

    // Note: methods are public while the class is package private.
    // This is to ensure the @PreAuthorize does its job as it's only guaranteed for public methods.
    @PreAuthorize("#employeeId == authentication.name")
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String employeeId) {
        // technically possible since token revocation isn't implemented yet
        User user = userRepository.findByEmployeeId(employeeId).orElseThrow(
                () -> new SystemIntegrityException("Critical system failure: " +
                        "Authenticated user record is missing from the database")
        );
        AdvisorStatisticsDTO advisorStats = null;

        if (user.getUserType() == UserType.ADVISOR) {
            AdvisorStatisticsProjection statsProjection = userRepository.getAdvisorStatistics(user.getId());
            advisorStats = new AdvisorStatisticsDTO(
                    statsProjection.getActiveClients(),
                    statsProjection.getActiveProducts()
            );
        }

        return userMapper.toProfileDto(user, advisorStats);
    }

    @PreAuthorize("#employeeId == authentication.name")
    @Transactional
    public UserProfileDTO updateUserProfile(String employeeId, UserUpdateDTO dto) {
        User user = userRepository.findByEmployeeId(employeeId).orElseThrow(
                () -> new SystemIntegrityException("Critical system failure: " +
                        "Authenticated user record is missing from the database")
        );

        if (!Objects.equals(user.getVersion(), dto.version())) {
            throw new ResourceVersionMismatchException("User record has been updated since last read. " +
                    "Please refresh and retry.");
        }

        if (!Objects.equals(dto.ico(), user.getIco()) && userRepository.existsByIco(dto.ico())) {
            throw new ResourceConflictException("User with this IČO already exists.");
        }

        user.setIco(dto.ico());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());

        AdvisorStatisticsDTO advisorStats = null;

        if (user.getUserType() == UserType.ADVISOR) {
            AdvisorStatisticsProjection statsProjection = userRepository.getAdvisorStatistics(user.getId());
            advisorStats = new AdvisorStatisticsDTO(
                    statsProjection.getActiveClients(),
                    statsProjection.getActiveProducts()
            );
        }

        return userMapper.toProfileDto(userRepository.saveAndFlush(user), advisorStats);
    }

    @Override
    public AdvisorSummaryDTO mapToAdvisorSummary(User user) {
        return userMapper.toAdvisorSummaryDto(user);
    }

}
