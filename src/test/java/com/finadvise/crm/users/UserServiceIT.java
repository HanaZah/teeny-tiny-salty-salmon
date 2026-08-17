package com.finadvise.crm.users;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.common.SystemIntegrityException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testAdvisor1;
    private User testAdvisor2;
    private User testAdmin;

    private final String testPassword = "secret";

    @BeforeAll
    void setUpAll() {
        cleanDatabase();

        testAdvisor1 = TestFixtureFactory.createIntegrationUser(
                100L, "IT-ADV-1", passwordEncoder.encode(testPassword), UserType.ADVISOR
        );
        userRepository.save(testAdvisor1);

        testAdvisor2 = TestFixtureFactory.createIntegrationUser(
                101L, "IT-ADV-2", passwordEncoder.encode(testPassword), UserType.ADVISOR
        );

        userRepository.save(testAdvisor2);

        testAdmin = TestFixtureFactory.createIntegrationAdmin(
                102L, "IT-ADM-1", passwordEncoder.encode(testPassword)
        );
        userRepository.save(testAdmin);
    }

    // --- 1. GET ADMIN CONTACT ---

    @Test
    void getAdminContact_ReturnsActiveAdminDetails() {
        UserContactDTO contact = userService.getAdminContact();

        assertNotNull(contact);
        assertEquals(testAdmin.getFirstName(), contact.firstName());
        assertEquals(testAdmin.getEmail(), contact.email());
    }

    // --- 2. UPDATE USER PROFILE ---
    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateUserProfile_OptimisticLocking_IncrementsVersion() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor1.getEmployeeId()).orElseThrow();
        Integer initialVersion = currentAdvisor.getVersion();

        String newFirst = "UpdatedFirst";
        String newLast = "UpdatedLast";
        String newPhone = "+420000111222";
        String newEmail = "update@finadvise.com";

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                initialVersion, currentAdvisor.getIco(), newFirst, newLast, newPhone, newEmail
        );

        userService.updateUserProfile(testAdvisor1.getEmployeeId(), updateDTO);

        User updatedAdvisor = userRepository.findByEmployeeId(testAdvisor1.getEmployeeId()).orElseThrow();

        assertEquals(newFirst, updatedAdvisor.getFirstName());
        assertEquals(initialVersion + 1, updatedAdvisor.getVersion());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateUserProfile_StaleData_ThrowsException() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor1.getEmployeeId()).orElseThrow();

        UserUpdateDTO staleDTO = new UserUpdateDTO(
                currentAdvisor.getVersion() - 1, currentAdvisor.getIco(), "Fail", "Fail", "+420000111222", "fail@finadvise.com"
        );

        assertThrows(ResourceVersionMismatchException.class, () ->
                userService.updateUserProfile(testAdvisor1.getEmployeeId(), staleDTO)
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateUserProfile_DuplicitIco_ThrowsException() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor1.getEmployeeId()).orElseThrow();

        UserUpdateDTO staleDTO = new UserUpdateDTO(
                currentAdvisor.getVersion(), testAdvisor2.getIco() , "Fail", "Fail", "+420000111222", "fail@finadvise.com"
        );

        assertThrows(ResourceConflictException.class, () ->
                userService.updateUserProfile(testAdvisor1.getEmployeeId(), staleDTO)
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-3", authorities = "ADVISOR")
    void updateUserProfile_UserMissing_ThrowsException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(
                0, "12345678", "UpdatedFirst", "UpdatedLast", "+420000111222", "update@finadvise.com"
        );

        assertThrows(SystemIntegrityException.class, () ->
                userService.updateUserProfile("IT-ADV-3", updateDTO)
        );
    }

    // --- 3. UPDATE USER PASSWORD ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR") // Assuming IT-ADV-1 is seeded
    void updateUserPassword_HappyPath_UpdatesHashInDatabase() {
        PasswordChangeDTO dto = new PasswordChangeDTO(testPassword, "NewStr0ngP@ss!");

        UserProfileDTO result = userService.updateUserPassword(dto, testAdvisor1.getEmployeeId());

        assertNotNull(result);

        User dbUser = userRepository.findById(testAdvisor1.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewStr0ngP@ss!", dbUser.getPasswordHash()));

        // Cleanup: revert the password so we don't break subsequent tests
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.forceUpdatePassword(testAdvisor1.getEmployeeId(), passwordEncoder.encode(testPassword));
        });
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateUserPassword_SamePasswordConflict_ThrowsException() {
        PasswordChangeDTO dto = new PasswordChangeDTO(testPassword, testPassword);

        assertThrows(ResourceConflictException.class, () ->
                userService.updateUserPassword(dto, testAdvisor1.getEmployeeId())
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateUserPassword_IncorrectCurrentPassword_ThrowsException() {
        PasswordChangeDTO dto = new PasswordChangeDTO("wrong-password", "NewStr0ngP@ss!");

        assertThrows(InvalidInputValueException.class, () ->
                userService.updateUserPassword(dto, testAdvisor1.getEmployeeId())
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateUserPassword_SecurityMismatch_ThrowsAccessDenied() {
        PasswordChangeDTO dto = new PasswordChangeDTO(testPassword, "NewStr0ngP@ss!");

        assertThrows(AccessDeniedException.class, () ->
                userService.updateUserPassword(dto, testAdvisor2.getEmployeeId())
        );
    }
}