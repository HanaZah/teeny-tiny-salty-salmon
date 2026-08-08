package com.finadvise.crm.users;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserAdministrationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserAdministrationService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testAdvisor;

    @BeforeAll
    void setUpAll() {
        userRepository.deleteAll();

        User testAdmin = TestFixtureFactory.createIntegrationAdmin(
                301L, "IT-ADM-1", passwordEncoder.encode("hash")
        );
        userRepository.save(testAdmin);

        testAdvisor = TestFixtureFactory.createIntegrationUser(
                302L, "IT-ADV-1", passwordEncoder.encode("hash"), UserType.ADVISOR, "11112222"
        );
        userRepository.save(testAdvisor);

        User testAdvisor2 = TestFixtureFactory.createIntegrationUser(
                303L, "IT-ADV-2", passwordEncoder.encode("hash"), UserType.ADVISOR, "33334444"
        );
        testAdvisor2.setFirstName("UniqueName");
        userRepository.save(testAdvisor2);
    }

    // --- GLOBAL SECURITY ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void globalSecurity_AdvisorAccess_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                adminService.getOrphanedPortfolios()
        );
    }

    // --- CREATE ADVISOR ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void createAdvisor_ValidData_PersistsUserAndReturnsPassword() {
        UserCreateDTO dto = new UserCreateDTO(
                "99998888", "New", "Advisor", "+420555444333", "new.adv@finadvise.com"
        );

        UserCredentialsInternalResult result = adminService.createAdvisor(dto);

        assertNotNull(result);
        assertNotNull(result.rawPassword());
        assertNotNull(result.userDetail().employeeId());

        User dbUser = userRepository.findByEmployeeId(result.userDetail().employeeId()).orElseThrow();
        assertEquals("new.adv@finadvise.com", dbUser.getEmail());
        assertEquals("99998888", dbUser.getIco());
        assertTrue(passwordEncoder.matches(result.rawPassword(), dbUser.getPasswordHash()));
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void createAdvisor_DuplicateIco_ThrowsConflictException() {
        UserCreateDTO dto = new UserCreateDTO(
                testAdvisor.getIco(), "Copy", "Cat", "+420000000000", "copy@finadvise.com"
        );

        assertThrows(ResourceConflictException.class, () ->
                adminService.createAdvisor(dto)
        );
    }

    // --- UPDATE USER STATUS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void updateUserStatus_ValidUpdate_ForcesChangeInDatabase() {
        UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO(false);

        UserDetailDTO result = adminService.updateUserStatus(testAdvisor.getEmployeeId(), updateDTO);

        assertFalse(result.isActive());

        User dbUser = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        assertFalse(dbUser.isActive());

        // Revert status for other tests
        adminService.updateUserStatus(testAdvisor.getEmployeeId(), new UserStatusUpdateDTO(true));
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void updateUserStatus_StaleVersion_BypassesOptimisticLocking() {
        User user = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        user.setPhone("+420999999999");
        userRepository.saveAndFlush(user); // Increments DB version

        UserStatusUpdateDTO updateDTO = new UserStatusUpdateDTO(false);
        assertDoesNotThrow(() -> adminService.updateUserStatus(testAdvisor.getEmployeeId(), updateDTO));

        User dbUser = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        assertFalse(dbUser.isActive());

        // Revert status
        adminService.updateUserStatus(testAdvisor.getEmployeeId(), new UserStatusUpdateDTO(true));
    }

    // --- RESET USER PASSWORD ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void resetUserPassword_ValidUser_UpdatesPassword() {
        String oldHash = testAdvisor.getPasswordHash();

        UserCredentialsInternalResult result = adminService.resetUserPassword(testAdvisor.getEmployeeId());

        User dbUser = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        assertNotEquals(oldHash, dbUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(result.rawPassword(), dbUser.getPasswordHash()));
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void resetUserPassword_StaleVersion_BypassesOptimisticLocking() {
        User user = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        user.setLastName("Modified");
        userRepository.saveAndFlush(user); // Increments DB version

        assertDoesNotThrow(() -> adminService.resetUserPassword(testAdvisor.getEmployeeId()));
    }

    // --- UPDATE USER EMAIL ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void updateUserEmail_ValidData_PersistsEmail() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        UserEmailUpdateDTO dto = new UserEmailUpdateDTO(currentAdvisor.getVersion(), "updated.e2e@finadvise.com");

        UserDetailDTO result = adminService.updateUserEmail(testAdvisor.getEmployeeId(), dto);

        assertEquals("updated.e2e@finadvise.com", result.email());

        User dbUser = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        assertEquals("updated.e2e@finadvise.com", dbUser.getEmail());
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void updateUserEmail_VersionMismatch_ThrowsException() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();

        UserEmailUpdateDTO staleDTO = new UserEmailUpdateDTO(
                currentAdvisor.getVersion() - 1, "stale@finadvise.com"
        );

        assertThrows(ResourceVersionMismatchException.class, () ->
                adminService.updateUserEmail(testAdvisor.getEmployeeId(), staleDTO)
        );
    }

    // --- ORPHANED PORTFOLIOS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void getOrphanedPortfolios_ExecutesSuccessfully() {
        OrphanedPortfoliosDTO result = adminService.getOrphanedPortfolios();

        assertNotNull(result);
        assertTrue(result.count() >= 0);
    }

    // --- SEARCH USERS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void searchUsers_ValidCriteria_ReturnsFilteredPage() {
        UserSearchCriteriaDTO criteria = new UserSearchCriteriaDTO("UniqueName", null, true);

        Page<UserSearchResultDTO> result = adminService.searchUsers(criteria, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("UniqueName", result.getContent().getFirst().firstName());
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void searchUsers_NoMatch_ReturnsEmptyPage() {
        UserSearchCriteriaDTO criteria = new UserSearchCriteriaDTO("NobodyByThisName", null, true);

        Page<UserSearchResultDTO> result = adminService.searchUsers(criteria, Pageable.unpaged());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    // --- ADVISOR SUGGESTIONS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void getAdvisorSuggestions_ValidName_ReturnsSuggestions() {
        AdvisorSuggestionRequestDTO request = new AdvisorSuggestionRequestDTO("Unique", 5);

        List<AdvisorSuggestionResultDTO> result = adminService.getAdvisorSuggestions(request);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().fullName().contains("UniqueName"));
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void getAdvisorSuggestions_NoMatch_ReturnsEmptyList() {
        AdvisorSuggestionRequestDTO request = new AdvisorSuggestionRequestDTO("NonexistentUser", 5);

        List<AdvisorSuggestionResultDTO> result = adminService.getAdvisorSuggestions(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}