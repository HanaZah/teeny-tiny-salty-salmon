package com.finadvise.crm.clients;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.addresses.AddressInputDTO;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientDetailOrchestratorIT extends AbstractIntegrationTest {

    @Autowired private ClientDetailOrchestrator orchestrator;

    // Spy on the service to verify delegation without breaking the actual integration flow
    @MockitoSpyBean private ClientService clientService;

    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ClientRepository clientRepository;

    private User testAdmin;
    private User testAdvisor1;
    private User testAdvisor2;
    private Address dummyAddress;

    private Client targetClient;
    private Client statusClient;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            dummyAddress = TestFixtureFactory.createIntegrationAddress(501);
            entityManager.persist(dummyAddress);

            testAdmin = TestFixtureFactory.createIntegrationAdmin(501L, "IT-ORCH-ADM-1", hash);
            testAdvisor1 = TestFixtureFactory.createIntegrationUser(502L, "IT-ORCH-ADV-1", hash, UserType.ADVISOR);
            testAdvisor2 = TestFixtureFactory.createIntegrationUser(503L, "IT-ORCH-ADV-2", hash, UserType.ADVISOR);

            entityManager.persist(testAdmin);
            entityManager.persist(testAdvisor1);
            entityManager.persist(testAdvisor2);

            targetClient = TestFixtureFactory.createIntegrationClient(501L, "UID-ORCH-1", testAdvisor1, dummyAddress);
            entityManager.persist(targetClient);

            statusClient = TestFixtureFactory.createIntegrationClient(502L, "UID-ORCH-2", testAdvisor1, dummyAddress);
            entityManager.persist(statusClient);

            entityManager.flush();
        });
    }

    @BeforeEach
    void resetSpy() {
        // Clear spy invocations between tests to ensure accurate verification counts
        Mockito.clearInvocations(clientService);
    }

    // --- 1. GLOBAL SECURITY CONSTRAINTS ---

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void globalSecurity_PrincipalMismatch_ThrowsAccessDenied() {
        // Advisor 1 attempting to pass Advisor 2's employee ID to bypass constraints
        assertThrows(AccessDeniedException.class, () ->
                orchestrator.getClientDetail(targetClient.getClientUid(), testAdvisor2.getEmployeeId(), false)
        );
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void globalSecurity_AdminOnly_ThrowsAccessDenied() {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false);

        // Advisor attempting to call an Admin-only endpoint
        assertThrows(AccessDeniedException.class, () ->
                orchestrator.updateClientStatus(dto, targetClient.getClientUid(), testAdvisor1.getEmployeeId())
        );
    }

    // --- 2. GET CLIENT DETAIL ---

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void getClientDetail_FullHydrationSuccess() {
        ClientDetailDTO result = orchestrator.getClientDetail(targetClient.getClientUid(), testAdvisor1.getEmployeeId(), false);

        assertNotNull(result);
        assertEquals(targetClient.getClientUid(), result.clientUid());
        assertNotNull(result.permanentAddress());
        assertNotNull(result.advisor());

        // Verify delegation
        verify(clientService).getDetailedClient(targetClient.getClientUid(), testAdvisor1.getEmployeeId(), false);
    }

    // --- 3. CREATE CLIENT ---

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void createClient_CrossModuleCreation_NewAddresses() {
        AddressInputDTO newAddress = new AddressInputDTO("Brand New St", "99", "New City", "111 00");
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801019999", LocalDate.now().minusYears(30), "New", "Guy", "IT", "+420", "new@finadvise.com",
                "123456789", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9), "MV CR",
                newAddress, newAddress
        );

        ClientDetailDTO result = orchestrator.createClient(dto, testAdvisor1.getEmployeeId());

        assertNotNull(result);
        assertEquals("Brand New St", result.permanentAddress().street());

        // Verify strict delegation of business logic
        verify(clientService).validateGeneralInfo(dto.birthDate(), dto.personalId(), null);
        verify(clientService).validateClientIdCard(dto.idCardExpiryDate(), dto.idCardIssueDate(), dto.birthDate(), dto.idCardNumber(), null);
        verify(clientService).createClient(eq(dto), any(User.class), any(Address.class), any(Address.class));
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void createClient_AddressDeduplicationIntegration() {
        // Passing exact values of an existing address
        AddressInputDTO duplicateAddress = new AddressInputDTO(
                dummyAddress.getStreet(), dummyAddress.getHouseNumber(), dummyAddress.getCity(), dummyAddress.getPostalCode()
        );
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801018888", LocalDate.now().minusYears(30), "Dedup", "Guy", "IT", "+420", "dedup@finadvise.com",
                "987654321", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9), "MV CR",
                duplicateAddress, duplicateAddress
        );

        ClientDetailDTO result = orchestrator.createClient(dto, testAdvisor1.getEmployeeId());

        assertNotNull(result);
        assertEquals(dummyAddress.getId(), result.permanentAddress().id()); // Proves it linked the existing ID

        // Verify delegation
        verify(clientService).validateGeneralInfo(any(), any(), any());
        verify(clientService).createClient(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void createClient_ValidationDelegationFailure_GeneralInfo() {
        AddressInputDTO dummyInput = new AddressInputDTO("S", "1", "C", "111 00");
        ClientCreateDTO dto = new ClientCreateDTO(
                "8801017777", LocalDate.now().minusYears(10), "Underage", "Kid", "IT", "+420", "kid@finadvise.com",
                "111222333", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9), "MV CR",
                dummyInput, dummyInput
        );

        // Expect validation failure directly from the service layer
        assertThrows(InvalidInputValueException.class, () ->
                orchestrator.createClient(dto, testAdvisor1.getEmployeeId())
        );

        // Prove the orchestrator correctly passed the buck to the service
        verify(clientService).validateGeneralInfo(dto.birthDate(), dto.personalId(), null);
    }

    // --- 4. UPDATE CLIENT GENERAL INFO ---

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void updateClientGeneralInfo_AddressModificationIntegration() {
        Client currentClient = clientRepository.findByClientUidWithDetails(targetClient.getClientUid()).orElseThrow();
        AddressInputDTO newAddress = new AddressInputDTO("Updated St", "42", "Upd City", "222 00");

        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                currentClient.getVersion(), currentClient.getPersonalId(), currentClient.getBirthDate(),
                "Updated", "Name", "IT", "+420", "upd@finadvise.com", newAddress, newAddress
        );

        ClientDetailDTO result = orchestrator.updateClientGeneralInfo(dto, currentClient.getClientUid(), testAdvisor1.getEmployeeId());

        assertNotNull(result);
        assertEquals("Updated St", result.permanentAddress().street());

        verify(clientService).validateGeneralInfo(dto.birthDate(), dto.personalId(), currentClient.getPersonalId());
        verify(clientService).updateGeneralInfo(eq(dto), any(Client.class), any(Address.class), any(Address.class));
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void updateClientGeneralInfo_NoOpAddressEfficiency() {
        Client currentClient = clientRepository.findByClientUidWithDetails(targetClient.getClientUid()).orElseThrow();
        Address existingAddr = currentClient.getResidentialAddress();

        AddressInputDTO exactSameAddress = new AddressInputDTO(
                existingAddr.getStreet(), existingAddr.getHouseNumber(), existingAddr.getCity(), existingAddr.getPostalCode()
        );

        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                currentClient.getVersion(), currentClient.getPersonalId(), currentClient.getBirthDate(),
                "SameAddr", "Name", "IT", "+420", "same@finadvise.com", exactSameAddress, exactSameAddress
        );

        ClientDetailDTO result = orchestrator.updateClientGeneralInfo(dto, currentClient.getClientUid(), testAdvisor1.getEmployeeId());

        assertNotNull(result);
        assertEquals(existingAddr.getId(), result.permanentAddress().id());

        // Verify it passed NULLs for the addresses, proving no-op efficiency
        verify(clientService).updateGeneralInfo(eq(dto), any(Client.class), eq(null), eq(null));
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void updateClientGeneralInfo_ValidationDelegationFailure() {
        Client currentClient = clientRepository.findByClientUidWithDetails(targetClient.getClientUid()).orElseThrow();
        AddressInputDTO exactSameAddress = new AddressInputDTO(
                currentClient.getResidentialAddress().getStreet(), currentClient.getResidentialAddress().getHouseNumber(),
                currentClient.getResidentialAddress().getCity(), currentClient.getResidentialAddress().getPostalCode()
        );

        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                currentClient.getVersion(), currentClient.getPersonalId(), LocalDate.now(), // Invalid Underage Date
                "Fail", "Name", "IT", "+420", "fail@finadvise.com", exactSameAddress, exactSameAddress
        );

        assertThrows(InvalidInputValueException.class, () ->
                orchestrator.updateClientGeneralInfo(dto, currentClient.getClientUid(), testAdvisor1.getEmployeeId())
        );

        verify(clientService).validateGeneralInfo(dto.birthDate(), dto.personalId(), currentClient.getPersonalId());
    }

    // --- 5. UPDATE CLIENT ID CARD ---

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void updateClientIdCard_OrchestrationFullCycle() {
        Client currentClient = clientRepository.findByClientUidWithDetails(targetClient.getClientUid()).orElseThrow();
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                currentClient.getVersion(), "999888777", LocalDate.now().minusYears(2), LocalDate.now().plusYears(8), "New Issuer"
        );

        ClientDetailDTO result = orchestrator.updateClientIdCard(dto, currentClient.getClientUid(), testAdvisor1.getEmployeeId());

        assertNotNull(result);
        assertEquals("999888777", result.idCardNumber());

        verify(clientService).validateClientIdCard(dto.idCardExpiryDate(), dto.idCardIssueDate(), currentClient.getBirthDate(), dto.idCardNumber(), currentClient.getIdCardNumber());
        verify(clientService).updateIdCard(eq(dto), any(Client.class));
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADV-1", authorities = "ADVISOR")
    void updateClientIdCard_ValidationDelegationFailure() {
        Client currentClient = clientRepository.findByClientUidWithDetails(targetClient.getClientUid()).orElseThrow();
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                currentClient.getVersion(), "999888777", LocalDate.now().minusYears(10), LocalDate.now().minusDays(1), "New Issuer" // Expired Card
        );

        assertThrows(InvalidInputValueException.class, () ->
                orchestrator.updateClientIdCard(dto, currentClient.getClientUid(), testAdvisor1.getEmployeeId())
        );

        verify(clientService).validateClientIdCard(dto.idCardExpiryDate(), dto.idCardIssueDate(), currentClient.getBirthDate(), dto.idCardNumber(), currentClient.getIdCardNumber());
    }

    // --- 6. UPDATE CLIENT STATUS ---

    @Test
    @WithMockUser(username = "IT-ORCH-ADM-1", authorities = "ADMIN")
    void updateClientStatus_AdminOrchestrationSuccess() {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false); // Disabling client

        ClientDetailDTO result = orchestrator.updateClientStatus(dto, statusClient.getClientUid(), testAdmin.getEmployeeId());

        assertNotNull(result);
        assertFalse(result.isActive());

        verify(clientService).updateStatus(eq(dto), any(Client.class));
    }

    @Test
    @WithMockUser(username = "IT-ORCH-ADM-1", authorities = "ADMIN")
    void updateClientStatus_ValidationDelegationFailure() {
        // Attempting to enable an already active client
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(true);

        assertThrows(InvalidInputValueException.class, () ->
                orchestrator.updateClientStatus(dto, statusClient.getClientUid(), testAdmin.getEmployeeId())
        );

        verify(clientService).updateStatus(eq(dto), any(Client.class));
    }
}