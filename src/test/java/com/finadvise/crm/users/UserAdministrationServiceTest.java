package com.finadvise.crm.users;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private Clock clock;
    @Mock private RandomSecureStringGenerator randomSecureStringGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailSender emailSender; // Kept to satisfy dependency injection, even if unused directly here
    @Mock private ObfuscatedIdGenerator idGenerator;

    @InjectMocks
    private UserAdministrationService adminService;

    private User mockAdvisor;
    private User mockAdmin;
    private UserDetailDTO mockDetailDTO;

    @BeforeEach
    void setUp() {
        mockAdvisor = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        mockAdmin = TestFixtureFactory.createValidUser("ADM-999", UserType.ADMIN);

        mockDetailDTO = new UserDetailDTO(
                mockAdvisor.getVersion(), mockAdvisor.getEmployeeId(),
                mockAdvisor.getFirstName(), mockAdvisor.getLastName(),
                mockAdvisor.getPhone(), mockAdvisor.getEmail(),
                mockAdvisor.getUserType(), mockAdvisor.isActive()
        );
    }

    // --- GET USER DETAIL ---

    @Test
    void getUserDetail_UserExists_ReturnsMappedProfile() {
        when(userRepository.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(userMapper.toDetailDto(mockAdvisor)).thenReturn(mockDetailDTO);

        UserDetailDTO result = adminService.getUserDetail(mockAdvisor.getEmployeeId());

        assertNotNull(result);
        assertEquals(mockAdvisor.getEmployeeId(), result.employeeId());
    }

    @Test
    void getUserDetail_UserMissing_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmployeeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.getUserDetail("UNKNOWN"));
    }

    // --- GET ORPHANED PORTFOLIOS ---

    @Test
    void getOrphanedPortfolios_ReturnsCount() {
        Instant fixedInstant = Instant.parse("2026-08-01T10:00:00Z");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        LocalDate expectedDate = LocalDate.now(Clock.fixed(fixedInstant, ZoneId.of("UTC")));

        when(userRepository.countOrphanedAdvisorPortfolios(expectedDate)).thenReturn(42L);

        OrphanedPortfoliosDTO result = adminService.getOrphanedPortfolios();

        assertNotNull(result);
        assertEquals(42L, result.count());
    }

    // --- SEARCH USERS ---

    @Test
    void searchUsers_ValidCriteria_ReturnsPage() {
        UserSearchCriteriaDTO criteria = new UserSearchCriteriaDTO("Doe", null, true);
        Page<User> userPage = new PageImpl<>(List.of(mockAdvisor));
        UserSearchResultDTO searchResultDTO = new UserSearchResultDTO(
                mockAdvisor.getEmployeeId(), mockAdvisor.getFirstName(),
                mockAdvisor.getLastName(), mockAdvisor.getIco(), mockAdvisor.isActive()
        );

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toSearchResultDto(mockAdvisor)).thenReturn(searchResultDTO);

        Page<UserSearchResultDTO> result = adminService.searchUsers(criteria, Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(mockAdvisor.getEmployeeId(), result.getContent().getFirst().employeeId());
    }

    // --- CREATE ADVISOR ---

    @Test
    void createAdvisor_ValidData_CreatesAndReturnsResult() {
        UserCreateDTO dto = new UserCreateDTO("12345678", "Jane", "Doe", "+420999888777", "jane.doe@finadvise.com");

        when(userRepository.existsByIco(dto.ico())).thenReturn(false);
        when(userRepository.getNextSequenceValue()).thenReturn(1001L);
        when(idGenerator.encode(1001L)).thenReturn("EMP-NEW");
        when(randomSecureStringGenerator.generateRandomPassword(8)).thenReturn("RawSecurePass1!");
        when(passwordEncoder.encode("RawSecurePass1!")).thenReturn("EncodedPass");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDetailDto(any(User.class))).thenReturn(mockDetailDTO);

        UserCredentialsInternalResult result = adminService.createAdvisor(dto);

        assertNotNull(result);
        assertEquals("RawSecurePass1!", result.rawPassword());
        assertEquals(mockDetailDTO, result.userDetail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(1001L, savedUser.getId());
        assertEquals("EMP-NEW", savedUser.getEmployeeId());
        assertEquals("EncodedPass", savedUser.getPasswordHash());
        assertEquals(UserType.ADVISOR, savedUser.getUserType());
    }

    @Test
    void createAdvisor_IcoExists_ThrowsConflictException() {
        UserCreateDTO dto = new UserCreateDTO("12345678", "Jane", "Doe", "+420999888777", "jane.doe@finadvise.com");
        when(userRepository.existsByIco(dto.ico())).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> adminService.createAdvisor(dto));
        verify(userRepository, never()).saveAndFlush(any());
    }

    // --- UPDATE USER STATUS ---

    @Test
    void updateUserStatus_ValidUpdate_ForcesUpdateAndReturnsDetail() {
        UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO(false); // Changing from true to false
        when(userRepository.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(userMapper.toDetailDto(mockAdvisor)).thenReturn(mockDetailDTO);

        UserDetailDTO result = adminService.updateUserStatus(mockAdvisor.getEmployeeId(), updateDTO);

        assertNotNull(result);
        verify(userRepository).forceUpdateStatus(mockAdvisor.getEmployeeId(), false);
        verify(userRepository, times(2)).findByEmployeeId(mockAdvisor.getEmployeeId()); // Checked once at start, once at end
    }

    @Test
    void updateUserStatus_AdminTarget_ThrowsException() {
        UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO(false);
        when(userRepository.findByEmployeeId(mockAdmin.getEmployeeId())).thenReturn(Optional.of(mockAdmin));

        assertThrows(InvalidInputValueException.class, () ->
                adminService.updateUserStatus(mockAdmin.getEmployeeId(), updateDTO)
        );
        verify(userRepository, never()).forceUpdateStatus(anyString(), anyBoolean());
    }

    @Test
    void updateUserStatus_RedundantStatus_ThrowsException() {
        UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO(true); // Already true
        when(userRepository.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));

        assertThrows(InvalidInputValueException.class, () ->
                adminService.updateUserStatus(mockAdvisor.getEmployeeId(), updateDTO)
        );
        verify(userRepository, never()).forceUpdateStatus(anyString(), anyBoolean());
    }

    @Test
    void updateUserStatus_UserMissing_ThrowsException() {
        UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO(false);
        when(userRepository.findByEmployeeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.updateUserStatus("UNKNOWN", updateDTO));
    }

    // --- RESET USER PASSWORD ---

    @Test
    void resetUserPassword_UserExists_UpdatesAndReturnsResult() {
        when(userRepository.existsByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(true);
        when(randomSecureStringGenerator.generateRandomPassword(8)).thenReturn("NewPass123!");
        when(passwordEncoder.encode("NewPass123!")).thenReturn("EncodedNewPass");
        when(userRepository.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(userMapper.toDetailDto(mockAdvisor)).thenReturn(mockDetailDTO);

        UserCredentialsInternalResult result = adminService.resetUserPassword(mockAdvisor.getEmployeeId());

        assertNotNull(result);
        assertEquals("NewPass123!", result.rawPassword());
        assertEquals(mockDetailDTO, result.userDetail());
        verify(userRepository).forceUpdatePassword(mockAdvisor.getEmployeeId(), "EncodedNewPass");
    }

    @Test
    void resetUserPassword_UserMissing_ThrowsException() {
        when(userRepository.existsByEmployeeId("UNKNOWN")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> adminService.resetUserPassword("UNKNOWN"));
        verify(userRepository, never()).forceUpdatePassword(anyString(), anyString());
    }

    // --- GET ADVISOR SUGGESTIONS ---

    @Test
    void getAdvisorSuggestions_ValidName_ReturnsList() {
        AdvisorSuggestionRequestDTO request = new AdvisorSuggestionRequestDTO("Doe", 10);
        AdvisorSuggestionResultDTO suggestion = new AdvisorSuggestionResultDTO("EMP-123", "John Doe");

        when(userRepository.findActiveAdvisorSuggestions("doe", 10)).thenReturn(List.of(suggestion));

        List<AdvisorSuggestionResultDTO> results = adminService.getAdvisorSuggestions(request);

        assertEquals(1, results.size());
        assertEquals("John Doe", results.getFirst().fullName());
    }

    @Test
    void getAdvisorSuggestions_NullName_NormalizesAndReturnsList() {
        AdvisorSuggestionRequestDTO request = new AdvisorSuggestionRequestDTO(null, 5);

        when(userRepository.findActiveAdvisorSuggestions("", 5)).thenReturn(List.of());

        List<AdvisorSuggestionResultDTO> results = adminService.getAdvisorSuggestions(request);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(userRepository).findActiveAdvisorSuggestions("", 5);
    }

    // --- UPDATE USER EMAIL ---

    @Test
    void updateUserEmail_ValidData_SavesAndReturnsDetail() {
        UserEmailUpdateDTO dto = new UserEmailUpdateDTO(mockAdvisor.getVersion(), "new.email@finadvise.com");

        when(userRepository.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(mockAdvisor);
        when(userMapper.toDetailDto(any(User.class))).thenReturn(mockDetailDTO);

        UserDetailDTO result = adminService.updateUserEmail(mockAdvisor.getEmployeeId(), dto);

        assertNotNull(result);
        assertEquals(dto.email(), mockAdvisor.getEmail());
        verify(userRepository).saveAndFlush(mockAdvisor);
    }

    @Test
    void updateUserEmail_VersionMismatch_ThrowsConflictException() {
        UserEmailUpdateDTO dto = new UserEmailUpdateDTO(mockAdvisor.getVersion() + 1, "new.email@finadvise.com");

        when(userRepository.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));

        assertThrows(ResourceVersionMismatchException.class, () ->
                adminService.updateUserEmail(mockAdvisor.getEmployeeId(), dto)
        );

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateUserEmail_UserMissing_ThrowsException() {
        UserEmailUpdateDTO dto = new UserEmailUpdateDTO(0, "new.email@finadvise.com");
        when(userRepository.findByEmployeeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.updateUserEmail("UNKNOWN", dto));
        verify(userRepository, never()).saveAndFlush(any());
    }
}