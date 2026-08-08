package com.finadvise.crm.users;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.common.SystemIntegrityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserProfileDTO mockProfileDTO;

    @BeforeEach
    void setUp() {
        mockUser = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        mockProfileDTO = new UserProfileDTO(
                mockUser.getVersion(), mockUser.getEmployeeId(), mockUser.getUserType(),
                mockUser.getFirstName(), mockUser.getLastName(),
                mockUser.getIco(), mockUser.getEmail(), mockUser.getPhone(), null
        );
    }

    @Test
    void getUserProfile_UserExists_ReturnsMappedProfile() {
        AdvisorStatisticsProjection mockProjection = mock(AdvisorStatisticsProjection.class);
        when(mockProjection.getActiveClients()).thenReturn(5);
        when(mockProjection.getActiveProducts()).thenReturn(10);

        when(userRepository.findByEmployeeId(mockUser.getEmployeeId())).thenReturn(Optional.of(mockUser));
        when(userRepository.getAdvisorStatistics(mockUser.getId())).thenReturn(mockProjection);
        when(userMapper.toProfileDto(any(User.class), any(AdvisorStatisticsDTO.class))).thenReturn(mockProfileDTO);

        UserProfileDTO result = userService.getUserProfile(mockUser.getEmployeeId());

        assertNotNull(result);
        assertEquals(mockUser.getEmployeeId(), result.employeeId());
        verify(userRepository).getAdvisorStatistics(mockUser.getId());
    }

    @Test
    void getUserProfile_UserMissing_ThrowsSystemIntegrityException() {
        String missingEmployeeId = "UNKNOWN";
        when(userRepository.findByEmployeeId(missingEmployeeId)).thenReturn(Optional.empty());

        assertThrows(SystemIntegrityException.class, () -> userService.getUserProfile(missingEmployeeId));
    }

    @Test
    void updateUserProfile_ValidUpdate_SavesAndReturnsProfile() {
        AdvisorStatisticsProjection mockProjection = mock(AdvisorStatisticsProjection.class);
        when(mockProjection.getActiveClients()).thenReturn(5);
        when(mockProjection.getActiveProducts()).thenReturn(10);
        UserUpdateDTO updateDTO = new UserUpdateDTO(
                mockUser.getVersion(), "Jane", "Smith", "+420987654321", "jane.smith@finadvise.com"
        );

        when(userRepository.findByEmployeeId(mockUser.getEmployeeId())).thenReturn(Optional.of(mockUser));
        when(userRepository.getAdvisorStatistics(mockUser.getId())).thenReturn(mockProjection);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(mockUser);
        when(userMapper.toProfileDto(any(User.class), any())).thenReturn(mockProfileDTO);

        UserProfileDTO result = userService.updateUserProfile(mockUser.getEmployeeId(), updateDTO);

        assertNotNull(result);
        assertEquals(updateDTO.firstName(), mockUser.getFirstName());
        assertEquals(updateDTO.lastName(), mockUser.getLastName());
        verify(userRepository).saveAndFlush(mockUser);
    }

    @Test
    void updateUserProfile_VersionMismatch_ThrowsConflictException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(
                mockUser.getVersion() + 1, "Jane", "Smith", "+420987654321", "jane.smith@finadvise.com"
        );

        when(userRepository.findByEmployeeId(mockUser.getEmployeeId())).thenReturn(Optional.of(mockUser));

        assertThrows(ResourceVersionMismatchException.class, () ->
                userService.updateUserProfile(mockUser.getEmployeeId(), updateDTO)
        );

        verify(userRepository, never()).saveAndFlush(any());
    }
}