package com.finadvise.crm.users;

import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.common.SystemIntegrityException;
import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
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
                () -> new SystemIntegrityException("error.system.admin-missing")
        );

        return userMapper.toContactDto(admin);
    }

    @PreAuthorize("#employeeId == authentication.name")
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String employeeId) {
        User user = userRepository.findByEmployeeId(employeeId).orElseThrow(
                () -> new SystemIntegrityException("error.system.user-missing")
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
                () -> new SystemIntegrityException("error.system.user-missing")
        );

        if (!Objects.equals(user.getVersion(), dto.version())) {
            throw new ResourceVersionMismatchException("error.concurrency.version-mismatch");
        }

        if (!Objects.equals(dto.ico(), user.getIco()) && userRepository.existsByIco(dto.ico())) {
            throw new ResourceConflictException("error.user.ico.conflict");
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

    @Override
    public List<StaticDictionaryItemDTO> getAllUserTypes() {
        return Arrays.stream(UserType.values()).map(userMapper::toStaticDictionaryItemDto).toList();
    }

    @Override
    public List<StaticDictionaryItemDTO> getAllUserStates() {
        return Arrays.stream(UserStatus.values()).map(userMapper::toStaticDictionaryItemDto).toList();
    }
}