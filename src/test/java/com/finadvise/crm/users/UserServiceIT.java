package com.finadvise.crm.users;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.common.SystemIntegrityException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testAdvisor;
    private User testAdmin;

    @BeforeAll
    void setUpAll() {
        userRepository.deleteAll();

        testAdvisor = TestFixtureFactory.createIntegrationUser(
                100L, "IT-ADV-1", "hash", UserType.ADVISOR, "12345678"
        );
        userRepository.save(testAdvisor);

        testAdmin = TestFixtureFactory.createIntegrationAdmin(
                101L, "IT-ADM-1", "hash"
        );
        userRepository.save(testAdmin);
    }

    @Test
    void getAdminContact_ReturnsActiveAdminDetails() {
        UserContactDTO contact = userService.getAdminContact();

        assertNotNull(contact);
        assertEquals(testAdmin.getFirstName(), contact.firstName());
        assertEquals(testAdmin.getEmail(), contact.email());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1")
    void updateUserProfile_OptimisticLocking_IncrementsVersion() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();
        Integer initialVersion = currentAdvisor.getVersion();

        String newFirst = "UpdatedFirst";
        String newLast = "UpdatedLast";
        String newPhone = "+420000111222";
        String newEmail = "update@finadvise.com";

        UserUpdateDTO updateDTO = new UserUpdateDTO(
                initialVersion, newFirst, newLast, newPhone, newEmail
        );

        userService.updateUserProfile(testAdvisor.getEmployeeId(), updateDTO);

        User updatedAdvisor = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();

        assertEquals(newFirst, updatedAdvisor.getFirstName());
        assertEquals(initialVersion + 1, updatedAdvisor.getVersion());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1")
    void updateUserProfile_StaleData_ThrowsException() {
        User currentAdvisor = userRepository.findByEmployeeId(testAdvisor.getEmployeeId()).orElseThrow();

        UserUpdateDTO staleDTO = new UserUpdateDTO(
                currentAdvisor.getVersion() - 1, "Fail", "Fail", "+420000111222", "fail@finadvise.com"
        );

        assertThrows(ResourceVersionMismatchException.class, () ->
                userService.updateUserProfile(testAdvisor.getEmployeeId(), staleDTO)
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-2")
    void updateUserProfile_UserMissing_ThrowsException() {
        UserUpdateDTO updateDTO = new UserUpdateDTO(
                0, "UpdatedFirst", "UpdatedLast", "+420000111222", "update@finadvise.com"
        );

        assertThrows(SystemIntegrityException.class, () ->
                userService.updateUserProfile("IT-ADV-2", updateDTO)
        );
    }
}