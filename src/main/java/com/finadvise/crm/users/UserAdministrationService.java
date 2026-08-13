package com.finadvise.crm.users;

import com.finadvise.crm.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserAdministrationService {
    static private final int MIN_PASSWORD_LENGTH = 8;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Clock clock;
    private final RandomSecureStringGenerator randomSecureStringGenerator;
    private final PasswordEncoder passwordEncoder;
    private final ObfuscatedIdGenerator idGenerator;

    @Transactional(readOnly = true)
    public UserDetailDTO getUserDetail(String employeeId) {
        return userMapper.toDetailDto(userRepository.findByEmployeeId(employeeId).orElseThrow(
                () -> new ResourceNotFoundException("error.user.not-found")
        ));
    }

    @Transactional(readOnly = true)
    public OrphanedPortfoliosDTO getOrphanedPortfolios() {
        return new OrphanedPortfoliosDTO(
                userRepository.countOrphanedAdvisorPortfolios(LocalDate.now(clock))
        );
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResultDTO> searchUsers(UserSearchCriteriaDTO criteria, Pageable pageable) {
        Specification<User> spec = UserSpecification.build(criteria);

        return userRepository.findAll(spec, pageable).map(userMapper::toSearchResultDto);
    }

    @Transactional
    public UserCredentialsInternalResult createAdvisor(UserCreateDTO dto) {
        if (userRepository.existsByIco(dto.ico())) {
            throw new ResourceConflictException("error.user.ico.conflict");
        }

        Long userId = userRepository.getNextSequenceValue();
        String employeeId = idGenerator.encode(userId);
        String password = randomSecureStringGenerator.generateRandomPassword(MIN_PASSWORD_LENGTH);

        User user = User.builder()
                .id(userId)
                .ico(dto.ico())
                .employeeId(employeeId)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .phone(dto.phone())
                .email(dto.email())
                .userType(UserType.ADVISOR)
                .isActive(true)
                .build();

        UserDetailDTO detail = userMapper.toDetailDto(userRepository.saveAndFlush(user));

        return new UserCredentialsInternalResult(detail, password);
    }

    @Transactional
    public UserDetailDTO updateUserStatus(String employeeId, UserStatusUpdateDTO dto) {
        User user = userRepository.findByEmployeeId(employeeId).orElseThrow(
                () -> new ResourceNotFoundException("error.user.not-found")
        );

        if (user.getUserType() == UserType.ADMIN) {
            throw new InvalidInputValueException("error.user.status.admin-immutable");
        }

        if (user.isActive() == dto.isActive()) {
            throw new InvalidInputValueException("error.user.status.redundant");
        }

        userRepository.forceUpdateStatus(employeeId, dto.isActive());

        return userMapper.toDetailDto(userRepository.findByEmployeeId(employeeId).orElseThrow());
    }

    @Transactional
    public UserCredentialsInternalResult resetUserPassword(String employeeId) {
        if (!userRepository.existsByEmployeeId(employeeId)) {
            throw new ResourceNotFoundException("error.user.not-found");
        }

        String password = randomSecureStringGenerator.generateRandomPassword(MIN_PASSWORD_LENGTH);
        userRepository.forceUpdatePassword(employeeId, passwordEncoder.encode(password));

        UserDetailDTO detail = userMapper.toDetailDto(userRepository.findByEmployeeId(employeeId).orElseThrow());

        return new UserCredentialsInternalResult(detail, password);
    }

    @Transactional(readOnly = true)
    public List<AdvisorSuggestionResultDTO> getAdvisorSuggestions(AdvisorSuggestionRequestDTO request) {
        String normalizedName = request.name() != null
                ? request.name().trim().toLowerCase()
                : "";

        return userRepository.findAdvisorSuggestions(normalizedName, request.limit());
    }

    @Transactional
    public UserDetailDTO updateUserEmail(String employeeId, UserEmailUpdateDTO dto) {
        User user = userRepository.findByEmployeeId(employeeId).orElseThrow(
                () -> new ResourceNotFoundException("error.user.not-found")
        );

        if (!user.getVersion().equals(dto.version())) {
            throw new ResourceVersionMismatchException("error.concurrency.version-mismatch");
        }

        user.setEmail(dto.email());
        return userMapper.toDetailDto(userRepository.saveAndFlush(user));
    }
}