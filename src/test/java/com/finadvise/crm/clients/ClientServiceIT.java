package com.finadvise.crm.clients;

import com.finadvise.crm.AbstractIntegrationTest;
import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.budget.Expense;
import com.finadvise.crm.budget.ExpenseType;
import com.finadvise.crm.budget.Income;
import com.finadvise.crm.budget.IncomeType;
import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.products.Product;
import com.finadvise.crm.products.ProductType;
import com.finadvise.crm.products.Provider;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientServiceIT extends AbstractIntegrationTest {

    @Autowired private ClientService clientService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testAdvisor1;
    private User testAdvisor2;
    private Address dummyAddress;

    private Client readClient1;
    private Client readClient2;
    private Client readClient3;
    private Client aggClient;
    private Client updateClient;
    private Client updateIdClient;
    private Client conflictClient;
    private Client statusClient;

    @BeforeAll
    void setUpAll() {
        transactionTemplate.executeWithoutResult(status -> {
            cleanDatabase();

            String hash = passwordEncoder.encode("secret");

            dummyAddress = TestFixtureFactory.createIntegrationAddress(101);
            entityManager.persist(dummyAddress);

            testAdmin = TestFixtureFactory.createIntegrationAdmin(201L, "IT-ADM-1", hash);
            testAdvisor1 = TestFixtureFactory.createIntegrationUser(202L, "IT-ADV-1", hash, UserType.ADVISOR);
            testAdvisor2 = TestFixtureFactory.createIntegrationUser(203L, "IT-ADV-2", hash, UserType.ADVISOR);

            entityManager.persist(testAdmin);
            entityManager.persist(testAdvisor1);
            entityManager.persist(testAdvisor2);

            // 1. Read-only Clients for Search and Suggestions
            readClient1 = TestFixtureFactory.createIntegrationClient(301L, "UID-R1", testAdvisor1, dummyAddress);
            readClient1.setFirstName("John");
            readClient1.setLastName("Doe");
            entityManager.persist(readClient1);

            readClient2 = TestFixtureFactory.createIntegrationClient(302L, "UID-R2", testAdvisor1, dummyAddress);
            readClient2.setFirstName("Jane");
            readClient2.setLastName("Doe");
            entityManager.persist(readClient2);

            readClient3 = TestFixtureFactory.createIntegrationClient(303L, "UID-R3", testAdvisor2, dummyAddress);
            readClient3.setFirstName("Bob");
            readClient3.setLastName("Smith");
            entityManager.persist(readClient3);

            // 2. Aggregation Client (For Overview Dashboard Statistics)
            aggClient = TestFixtureFactory.createIntegrationClient(304L, "UID-AGG", testAdvisor1, dummyAddress);
            entityManager.persist(aggClient);

            // 3. Isolated Clients for Update Testing
            updateClient = TestFixtureFactory.createIntegrationClient(305L, "UID-UPD1", testAdvisor1, dummyAddress);
            entityManager.persist(updateClient);

            updateIdClient = TestFixtureFactory.createIntegrationClient(306L, "UID-UPD2", testAdvisor1, dummyAddress);
            entityManager.persist(updateIdClient);

            conflictClient = TestFixtureFactory.createIntegrationClient(307L, "UID-CFL", testAdvisor1, dummyAddress);
            conflictClient.setPersonalId("9999999999");
            conflictClient.setIdCardNumber("999999999");
            entityManager.persist(conflictClient);

            statusClient = TestFixtureFactory.createIntegrationClient(308L, "UID-STAT", testAdvisor1, dummyAddress);
            entityManager.persist(statusClient);

            entityManager.flush();

            // 4. Seed Cross-Domain Aggregation Data (Products, Incomes, Expenses)
            IncomeType it1 = IncomeType.builder().name("IT1").build();
            IncomeType it2 = IncomeType.builder().name("IT2").build();
            ExpenseType et1 = ExpenseType.builder().name("ET1").build();
            entityManager.persist(it1);
            entityManager.persist(it2);
            entityManager.persist(et1);

            Income inc1 = TestFixtureFactory.createIntegrationIncome(aggClient, it1, 5000);
            Income inc2 = TestFixtureFactory.createIntegrationIncome(aggClient, it2, 2000);
            Expense exp1 = TestFixtureFactory.createIntegrationExpense(aggClient, et1, 1000, true);
            entityManager.persist(inc1);
            entityManager.persist(inc2);
            entityManager.persist(exp1);

            ProductType pt1 = ProductType.builder().name("PT1").build();
            Provider prov1 = Provider.builder().name("PROV1").build();
            entityManager.persist(pt1);
            entityManager.persist(prov1);

            // 2 Active Products, 1 Expired Product
            Product p1 = Product.builder().name("P1").amount(BigDecimal.TEN).startDate(LocalDate.now().minusDays(10)).endDate(null)
                    .productType(pt1).provider(prov1).client(aggClient).advisor(testAdvisor1).build();
            Product p2 = Product.builder().name("P2").amount(BigDecimal.TEN).startDate(LocalDate.now().minusDays(5)).endDate(LocalDate.now().plusMonths(1))
                    .productType(pt1).provider(prov1).client(aggClient).advisor(testAdvisor1).build();
            Product pExpired = Product.builder().name("P3").amount(BigDecimal.TEN).startDate(LocalDate.now().minusDays(20)).endDate(LocalDate.now().minusDays(5))
                    .productType(pt1).provider(prov1).client(aggClient).advisor(testAdvisor1).build();

            entityManager.persist(p1);
            entityManager.persist(p2);
            entityManager.persist(pExpired);
        });
    }

    // --- 1. RECENT CLIENT OVERVIEWS ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void getRecentClientOverviews_ComplexAggregationSuccess() {
        List<ClientOverviewDTO> overviews = clientService.getRecentClientOverviews(testAdvisor1.getEmployeeId(), 10);

        // Find our specific aggregation client in the returned payload
        Optional<ClientOverviewDTO> aggResult = overviews.stream()
                .filter(o -> o.clientUid().equals(aggClient.getClientUid()))
                .findFirst();

        assertTrue(aggResult.isPresent());
        ClientStatisticsDTO stats = aggResult.get().statistics();

        // Verifies the database view correctly calculated 2 active products (ignoring 1 expired),
        // and aggregated cashflow: (5000 + 2000) - 1000 = 6000
        assertEquals(2L, stats.activeProducts());
        assertEquals(6000L, stats.cashFlow());
    }

    // --- 2. GET CLIENT SUGGESTIONS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void getClientSuggestions_AdminVisibility_GlobalSearch() {
        ClientSuggestionRequestDTO request = new ClientSuggestionRequestDTO("Doe", 10);
        List<ClientSuggestionResultDTO> results = clientService.getClientSuggestions(request, testAdmin.getEmployeeId(), true);

        // Admin should see both Doe clients (readClient1 and readClient2) regardless of advisor
        assertTrue(results.stream().anyMatch(r -> r.clientUid().equals(readClient1.getClientUid())));
        assertTrue(results.stream().anyMatch(r -> r.clientUid().equals(readClient2.getClientUid())));
    }

    @Test
    @WithMockUser(username = "IT-ADV-2", authorities = "ADVISOR")
    void getClientSuggestions_AdvisorVisibility_IsolatedSearch() {
        // "Doe" clients belong to Advisor 1. Advisor 2 should get 0 results for "Doe"
        ClientSuggestionRequestDTO requestDoe = new ClientSuggestionRequestDTO("Doe", 10);
        List<ClientSuggestionResultDTO> resultsDoe = clientService.getClientSuggestions(requestDoe, testAdvisor2.getEmployeeId(), false);
        assertTrue(resultsDoe.isEmpty());

        // Advisor 2 searches for "Smith" (belongs to them)
        ClientSuggestionRequestDTO requestSmith = new ClientSuggestionRequestDTO("Smith", 10);
        List<ClientSuggestionResultDTO> resultsSmith = clientService.getClientSuggestions(requestSmith, testAdvisor2.getEmployeeId(), false);
        assertEquals(1, resultsSmith.size());
        assertEquals(readClient3.getClientUid(), resultsSmith.getFirst().clientUid());
    }

    // --- 3. GET DETAILED CLIENT ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void getDetailedClient_EntityGraphResolution() {
        // Method runs outside of a transaction
        Client client = clientService.getDetailedClient(readClient1.getClientUid(), testAdvisor1.getEmployeeId(), false);

        assertNotNull(client);
        // Asserting relationships to guarantee they are eagerly fetched.
        // If the EntityGraph failed, these would throw LazyInitializationException here.
        assertNotNull(client.getAdvisor().getEmployeeId());
        assertNotNull(client.getResidentialAddress().getCity());
        assertNotNull(client.getContactAddress().getCity());
    }

    // --- 4. SEARCH CLIENTS ---

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void searchClients_DatabaseViewIntegration_Admin() {
        ClientSearchCriteriaDTO criteria = new ClientSearchCriteriaDTO(null, "John", null, "Integration City", null);
        Page<ClientSearchResultDTO> result = clientService.searchClients(criteria, Pageable.unpaged(), testAdmin.getEmployeeId(), true);

        assertEquals(1, result.getTotalElements());
        assertEquals(readClient1.getClientUid(), result.getContent().getFirst().clientUid());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void searchClients_SecurityOverride_Advisor() {
        // Advisor 1 attempts to maliciously search for Advisor 2's clients
        ClientSearchCriteriaDTO maliciousCriteria = new ClientSearchCriteriaDTO(testAdvisor2.getEmployeeId(), null, null, null, null);

        Page<ClientSearchResultDTO> result = clientService.searchClients(maliciousCriteria, Pageable.unpaged(), testAdvisor1.getEmployeeId(), false);

        // Verify the malicious ID was overridden by the principal ID (Advisor 1).
        // Only Advisor 1's clients should be returned.
        assertTrue(result.getTotalElements() > 0);
        assertTrue(result.getContent().stream().noneMatch(c -> c.clientUid().equals(readClient3.getClientUid())));
    }

    // --- 5. CREATE CLIENT ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void createClient_DatabasePersistenceAndIdGeneration() {
        ClientCreateDTO dto = new ClientCreateDTO(
                "1101010000", LocalDate.of(1990, 1, 1), "New", "Client", "IT",
                "+420000", "new@finadvise.com", "111111111", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9),
                "MV CR", null, null
        );

        Client newClient = clientService.createClient(dto, testAdvisor1, dummyAddress, dummyAddress);

        assertNotNull(newClient.getId());
        assertNotNull(newClient.getClientUid());

        Client dbClient = clientRepository.findById(newClient.getId()).orElseThrow();
        assertEquals("New", dbClient.getFirstName());
        assertEquals(testAdvisor1.getId(), dbClient.getAdvisor().getId());
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void createClient_UniqueConstraintViolation_PersonalId() {
        ClientCreateDTO conflictDto = new ClientCreateDTO(
                conflictClient.getPersonalId(), // Already exists!
                LocalDate.of(1990, 1, 1), "New", "Client", "IT",
                "+420000", "new@finadvise.com", "222222222", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9),
                "MV CR", null, null
        );

        assertThrows(ResourceConflictException.class, () ->
                clientService.validateGeneralInfo(conflictDto.birthDate(), conflictDto.personalId(), null)
        );
    }

    // --- 6. VALIDATE GENERAL INFO ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void validateGeneralInfo_UniqueConstraintViolation_PersonalId() {
        Client currentClient = clientRepository.findById(Objects.requireNonNull(updateIdClient.getId())).orElseThrow();

        assertThrows(ResourceConflictException.class, () ->
                clientService.validateGeneralInfo(currentClient.getBirthDate(), conflictClient.getPersonalId(), currentClient.getPersonalId())
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void validateGeneralInfo_UniqueConstraintBypass_SelfMatch() {
        Client currentClient = clientRepository.findById(Objects.requireNonNull(updateIdClient.getId())).orElseThrow();

        // Ensure passing their own existing personal ID does not trigger a false positive
        assertDoesNotThrow(() ->
                clientService.validateGeneralInfo(currentClient.getBirthDate(), currentClient.getPersonalId(), currentClient.getPersonalId())
        );
    }

    // --- 7. UPDATE GENERAL INFO ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateGeneralInfo_OptimisticLocking_VersionMismatch() {
        Integer originalVersion = updateClient.getVersion();

        // Manually mutate the client in the DB to increment the version
        transactionTemplate.executeWithoutResult(s -> {
            Client c = clientRepository.findById(Objects.requireNonNull(updateClient.getId())).orElseThrow();
            c.setFirstName("Mutated By Another TX");
            clientRepository.saveAndFlush(c);
        });

        ClientGeneralUpdateDTO staleDto = new ClientGeneralUpdateDTO(
                originalVersion, "9999888877", LocalDate.of(1990, 1, 1), "Stale", "Name",
                "IT", "+420", "stale@finadvise.com", null, null
        );

        Client reFetchedClient = clientRepository.findByClientUidWithDetails(updateClient.getClientUid()).orElseThrow();

        assertThrows(ResourceVersionMismatchException.class, () ->
                clientService.updateGeneralInfo(staleDto, reFetchedClient, null, null)
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-2", authorities = "ADVISOR")
    void updateGeneralInfo_SpelOwnershipEnforcement() {
        Client clientOwnedByAdv1 = clientRepository.findByClientUidWithDetails(updateClient.getClientUid()).orElseThrow();
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                clientOwnedByAdv1.getVersion(), "9999888877", LocalDate.of(1990, 1, 1), "Stale", "Name",
                "IT", "+420", "stale@finadvise.com", null, null
        );

        // Advisor 2 attempting to update Advisor 1's client
        assertThrows(AccessDeniedException.class, () ->
                clientService.updateGeneralInfo(dto, clientOwnedByAdv1, null, null)
        );
    }

    // --- 8. VALIDATE ID CARD ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void validateIdCard_UniqueConstraintViolation() {
        Client currentClient = clientRepository.findById(Objects.requireNonNull(updateIdClient.getId())).orElseThrow();

        assertThrows(ResourceConflictException.class, () ->
                clientService.validateClientIdCard(
                        LocalDate.now().plusYears(1), LocalDate.now().minusYears(1), currentClient.getBirthDate(),
                        conflictClient.getIdCardNumber(), currentClient.getIdCardNumber())
        );
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void validateIdCard_UniqueConstraintBypass_SelfMatch() {
        Client currentClient = clientRepository.findById(Objects.requireNonNull(updateIdClient.getId())).orElseThrow();

        // Updating with their own ID card number
        assertDoesNotThrow(() ->
                clientService.validateClientIdCard(
                        LocalDate.now().plusYears(1), LocalDate.now().minusYears(1), currentClient.getBirthDate(),
                        currentClient.getIdCardNumber(), currentClient.getIdCardNumber())
        );
    }

    // --- 9. UPDATE ID CARD ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateIdCard_DatabasePersistence() {
        Client currentClient = clientRepository.findByClientUidWithDetails(updateIdClient.getClientUid()).orElseThrow();
        Integer originalVersion = currentClient.getVersion();

        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                originalVersion, "100200300", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9), "New Issuer"
        );

        clientService.updateIdCard(dto, currentClient);

        Client dbClient = clientRepository.findById(Objects.requireNonNull(updateIdClient.getId())).orElseThrow();
        assertEquals("100200300", dbClient.getIdCardNumber());
        assertEquals("New Issuer", dbClient.getIdCardIssuer());
        assertEquals(originalVersion + 1, dbClient.getVersion()); // Verify optimistic lock was bumped
    }

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateIdCard_OptimisticLocking_VersionMismatch() {
        Client currentClient = clientRepository.findByClientUidWithDetails(updateIdClient.getClientUid()).orElseThrow();
        Integer staleVersion = currentClient.getVersion() - 1;

        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                staleVersion, "100200300", LocalDate.now().minusYears(1), LocalDate.now().plusYears(9), "New Issuer"
        );

        assertThrows(ResourceVersionMismatchException.class, () ->
                clientService.updateIdCard(dto, currentClient)
        );
    }

    // --- 10. UPDATE STATUS ---

    @Test
    @WithMockUser(username = "IT-ADV-1", authorities = "ADVISOR")
    void updateStatus_GlobalSecurityConstraint_AdminOnly() {
        Client currentClient = clientRepository.findById(Objects.requireNonNull(statusClient.getId())).orElseThrow();
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false);

        assertThrows(AccessDeniedException.class, () ->
                clientService.updateStatus(dto, currentClient)
        );
    }

    @Test
    @WithMockUser(username = "IT-ADM-1", authorities = "ADMIN")
    void updateStatus_DatabasePersistence() {
        Client currentClient = clientRepository.findById(Objects.requireNonNull(statusClient.getId())).orElseThrow();
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false); // Disable client

        clientService.updateStatus(dto, currentClient);

        Client dbClient = clientRepository.findById(statusClient.getId()).orElseThrow();
        assertFalse(dbClient.isActive());
    }
}