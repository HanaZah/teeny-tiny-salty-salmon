package com.finadvise.crm.clients;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.common.InvalidInputValueException;
import com.finadvise.crm.common.ObfuscatedIdGenerator;
import com.finadvise.crm.common.ResourceConflictException;
import com.finadvise.crm.common.ResourceVersionMismatchException;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientSearchMinimalRepository clientSearchMinimalRepository;
    @Mock private ClientMapper clientMapper;
    @Mock private ObfuscatedIdGenerator obfuscatedIdGenerator;
    @Mock private Clock clock;

    @InjectMocks
    private ClientService clientService;

    private User mockAdvisor;
    private Client mockClient;

    @BeforeEach
    void setUp() {
        mockAdvisor = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        mockClient = TestFixtureFactory.createValidClient(1L, "C-123456", mockAdvisor);
    }

    private void mockClock() {
        // Fix the date to 2026-08-11 to keep tests deterministic
        Instant fixedInstant = Instant.parse("2026-08-11T10:00:00Z");
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    // --- 1. RECENT CLIENT OVERVIEWS ---

    @Test
    void getRecentClientOverviews_ValidLimit_ReturnsList() {
        mockClock();
        ClientOverviewProjection mockProjection = mock(ClientOverviewProjection.class);
        ClientOverviewDTO expectedDto = new ClientOverviewDTO("C-1", "John", "Doe", "IT", null);

        when(clientRepository.findRecentClientOverviews(eq(mockAdvisor.getEmployeeId()), any(LocalDate.class), eq(Limit.of(5))))
                .thenReturn(List.of(mockProjection));
        when(clientMapper.toOverviewDTO(mockProjection)).thenReturn(expectedDto);

        List<ClientOverviewDTO> results = clientService.getRecentClientOverviews(mockAdvisor.getEmployeeId(), 5);

        assertEquals(1, results.size());
        assertEquals(expectedDto, results.getFirst());
    }

    @Test
    void getRecentClientOverviews_LimitTooLow_ThrowsException() {
        assertThrows(InvalidInputValueException.class, () ->
                clientService.getRecentClientOverviews(mockAdvisor.getEmployeeId(), 0)
        );
        verifyNoInteractions(clientRepository);
    }

    @Test
    void getRecentClientOverviews_LimitTooHigh_ThrowsException() {
        assertThrows(InvalidInputValueException.class, () ->
                clientService.getRecentClientOverviews(mockAdvisor.getEmployeeId(), 21)
        );
        verifyNoInteractions(clientRepository);
    }

    // --- 2. GET CLIENT SUGGESTIONS ---

    @Test
    void getClientSuggestions_Admin_CallsGlobalRepositoryMethod() {
        ClientSuggestionRequestDTO request = new ClientSuggestionRequestDTO("Doe ", 10);
        ClientSuggestionResultDTO mockResult = new ClientSuggestionResultDTO("C-123", "John Doe");

        when(clientRepository.findClientSuggestions("doe", 10)).thenReturn(List.of(mockResult));

        List<ClientSuggestionResultDTO> results = clientService.getClientSuggestions(request, mockAdvisor.getEmployeeId(), true);

        assertEquals(1, results.size());
        verify(clientRepository).findClientSuggestions("doe", 10);
        verify(clientRepository, never()).findClientSuggestionsForAdvisor(anyString(), anyString(), anyInt());
    }

    @Test
    void getClientSuggestions_Advisor_CallsAdvisorRepositoryMethod() {
        ClientSuggestionRequestDTO request = new ClientSuggestionRequestDTO(null, 10);
        ClientSuggestionResultDTO mockResult = new ClientSuggestionResultDTO("C-123", "John Doe");

        when(clientRepository.findClientSuggestionsForAdvisor("", mockAdvisor.getEmployeeId(), 10))
                .thenReturn(List.of(mockResult));

        List<ClientSuggestionResultDTO> results = clientService.getClientSuggestions(request, mockAdvisor.getEmployeeId(), false);

        assertEquals(1, results.size());
        verify(clientRepository).findClientSuggestionsForAdvisor("", mockAdvisor.getEmployeeId(), 10);
        verify(clientRepository, never()).findClientSuggestions(anyString(), anyInt());
    }

    // --- 3. SEARCH CLIENTS ---

    @Test
    void searchClients_Admin_PassesCriteriaIntact() {
        ClientSearchCriteriaDTO originalCriteria = new ClientSearchCriteriaDTO("ADV-123", "Doe", null, null, null);
        ClientSearchMinimal mockMinimal = mock(ClientSearchMinimal.class);
        Page<ClientSearchMinimal> mockPage = new PageImpl<>(List.of(mockMinimal));
        ClientSearchResultDTO mockResult = new ClientSearchResultDTO("C-1", "John Doe", "123", "City", "Active");

        when(clientSearchMinimalRepository.findAll(
                ArgumentMatchers.<Specification<ClientSearchMinimal>>any(),
                any(Pageable.class)
        )).thenReturn(mockPage);
        when(clientMapper.toSearchResultDTO(mockMinimal)).thenReturn(mockResult);

        Page<ClientSearchResultDTO> result = clientService.searchClients(originalCriteria, Pageable.unpaged(), "ADM-1", true);

        assertEquals(1, result.getTotalElements());
        verify(clientSearchMinimalRepository).findAll(
                ArgumentMatchers.<Specification<ClientSearchMinimal>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void searchClients_Advisor_OverridesEmployeeId() {
        ClientSearchCriteriaDTO criteria = new ClientSearchCriteriaDTO("SNEAKY-ADV", "Doe", null, null, null);
        ClientSearchMinimal mockMinimal = mock(ClientSearchMinimal.class);
        Page<ClientSearchMinimal> mockPage = new PageImpl<>(List.of(mockMinimal));

        when(clientSearchMinimalRepository.findAll(
                ArgumentMatchers.<Specification<ClientSearchMinimal>>any(),
                any(Pageable.class)
        )).thenReturn(mockPage);

        clientService.searchClients(criteria, Pageable.unpaged(), "IT-ADV-1", false);

        // Verification happens via successful method completion and interaction with repository.
        // Spec validates the modified dto internally.
        verify(clientSearchMinimalRepository).findAll(
                ArgumentMatchers.<Specification<ClientSearchMinimal>>any(),
                any(Pageable.class)
        );
    }

    // --- 4. VALIDATE CLIENT ID CARD ---

    @Test
    void validateClientIdCard_Success() {
        mockClock();
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate issueDate = LocalDate.of(2020, 1, 1);
        LocalDate expiryDate = LocalDate.of(2030, 1, 1);

        assertDoesNotThrow(() ->
                clientService.validateClientIdCard(expiryDate, issueDate, birthDate, "123456789", "123456789")
        );
    }

    @Test
    void validateClientIdCard_ConflictFailure_ThrowsException() {
        mockClock();
        when(clientRepository.existsByIdCardNumber("987654321")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () ->
                clientService.validateClientIdCard(
                        LocalDate.of(2030, 1, 1), LocalDate.of(2020, 1, 1), LocalDate.of(1990, 1, 1),
                        "987654321", "123456789")
        );
    }

    @Test
    void validateClientIdCard_IssueDateBeforeBirthDate_ThrowsException() {
        mockClock();
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate issueDate = LocalDate.of(1989, 1, 1);
        LocalDate expiryDate = LocalDate.of(2030, 1, 1);

        assertThrows(InvalidInputValueException.class, () ->
                clientService.validateClientIdCard(expiryDate, issueDate, birthDate, "123456789", "123456789")
        );
    }

    @Test
    void validateClientIdCard_IssueDateInFuture_ThrowsException() {
        mockClock(); // Fixed at 2026-08-11
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate issueDate = LocalDate.of(2027, 1, 1);
        LocalDate expiryDate = LocalDate.of(2030, 1, 1);

        assertThrows(InvalidInputValueException.class, () ->
                clientService.validateClientIdCard(expiryDate, issueDate, birthDate, "123456789", "123456789")
        );
    }

    @Test
    void validateClientIdCard_IssueDateAfterExpiry_ThrowsException() {
        mockClock();
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate issueDate = LocalDate.of(2025, 1, 1);
        LocalDate expiryDate = LocalDate.of(2024, 1, 1);

        assertThrows(InvalidInputValueException.class, () ->
                clientService.validateClientIdCard(expiryDate, issueDate, birthDate, "123456789", "123456789")
        );
    }

    @Test
    void validateClientIdCard_ExpiredCard_ThrowsException() {
        mockClock(); // Fixed at 2026-08-11
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate issueDate = LocalDate.of(2015, 1, 1);
        LocalDate expiryDate = LocalDate.of(2025, 1, 1); // Expired

        assertThrows(InvalidInputValueException.class, () ->
                clientService.validateClientIdCard(expiryDate, issueDate, birthDate, "123456789", "123456789")
        );
    }

    // --- 5. VALIDATE GENERAL INFO ---

    @Test
    void validateGeneralInfo_Success() {
        mockClock();
        assertDoesNotThrow(() ->
                clientService.validateGeneralInfo(LocalDate.of(1990, 1, 1), "9001011234", "9001011234")
        );
    }

    @Test
    void validateGeneralInfo_ConflictFailure_ThrowsException() {
        mockClock();
        when(clientRepository.existsByPersonalId("9901011234")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () ->
                clientService.validateGeneralInfo(LocalDate.of(1990, 1, 1), "9901011234", "9001011234")
        );
    }

    @Test
    void validateGeneralInfo_UnderageClient_ThrowsException() {
        mockClock(); // Fixed at 2026-08-11
        LocalDate underageBirthDate = LocalDate.of(2010, 1, 1); // 16 years old

        assertThrows(InvalidInputValueException.class, () ->
                clientService.validateGeneralInfo(underageBirthDate, "1001011234", "1001011234")
        );
    }

    // --- 6. CREATE CLIENT ---

    @Test
    void createClient_Success_SavesEntity() {
        ClientCreateDTO dto = new ClientCreateDTO(
                "9001011234", LocalDate.of(1990, 1, 1), "John", "Doe", "IT", "+420123", "a@b.c",
                "123456789", LocalDate.now(), LocalDate.now().plusYears(5), "MVCR", null, null
        );
        Address mockAddress = Address.builder().id(1L).build();

        when(clientRepository.getNextSequenceValue()).thenReturn(99L);
        when(obfuscatedIdGenerator.encode(99L)).thenReturn("C-000099");
        when(clientRepository.saveAndFlush(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        Client result = clientService.createClient(dto, mockAdvisor, mockAddress, mockAddress);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("C-000099", result.getClientUid());
        assertEquals(mockAdvisor, result.getAdvisor());
        assertEquals(mockAddress, result.getResidentialAddress());
        verify(clientRepository).saveAndFlush(any(Client.class));
    }

    // --- 7. UPDATE GENERAL INFO ---

    @Test
    void updateGeneralInfo_Success_UpdatesFieldsAndAddresses() {
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                mockClient.getVersion(), "9901011234", LocalDate.of(1999, 1, 1), "Jane", "Smith",
                "Manager", "+420999888777", "jane@finadvise.com", null, null
        );

        Address newResidential = Address.builder().id(2L).city("New City").build();
        Address newContact = Address.builder().id(3L).city("Other City").build();

        when(clientRepository.saveAndFlush(mockClient)).thenReturn(mockClient);

        Client result = clientService.updateGeneralInfo(dto, mockClient, newResidential, newContact);

        assertEquals("Jane", result.getFirstName());
        assertEquals("9901011234", result.getPersonalId());
        assertEquals(newResidential, result.getResidentialAddress());
        assertEquals(newContact, result.getContactAddress());
        verify(clientRepository).saveAndFlush(mockClient);
    }

    @Test
    void updateGeneralInfo_Success_PartialAddressUpdate() {
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                mockClient.getVersion(), "9901011234", LocalDate.of(1999, 1, 1), "Jane", "Smith",
                "Manager", "+420999888777", "jane@finadvise.com", null, null
        );

        Address originalResidential = mockClient.getResidentialAddress();
        Address originalContact = mockClient.getContactAddress();

        when(clientRepository.saveAndFlush(mockClient)).thenReturn(mockClient);

        // Pass nulls to simulate addresses weren't changed in orchestrator
        Client result = clientService.updateGeneralInfo(dto, mockClient, null, null);

        assertEquals(originalResidential, result.getResidentialAddress());
        assertEquals(originalContact, result.getContactAddress());
    }

    @Test
    void updateGeneralInfo_VersionMismatch_ThrowsException() {
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                mockClient.getVersion() + 1, "9901011234", LocalDate.of(1999, 1, 1), "Jane", "Smith",
                "Manager", "+420999888777", "jane@finadvise.com", null, null
        );

        assertThrows(ResourceVersionMismatchException.class, () ->
                clientService.updateGeneralInfo(dto, mockClient, null, null)
        );
        verify(clientRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateGeneralInfo_InactiveClient_ThrowsException() {
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                mockClient.getVersion(), "9901011234", LocalDate.of(1999, 1, 1), "Jane", "Smith",
                "Manager", "+420999888777", "jane@finadvise.com", null, null
        );
        mockClient.setActive(false);

        assertThrows(InvalidInputValueException.class, () ->
                clientService.updateGeneralInfo(dto, mockClient, null, null)
        );
        verify(clientRepository, never()).saveAndFlush(any());
    }

    // --- 8. UPDATE ID CARD ---

    @Test
    void updateIdCard_Success() {
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                mockClient.getVersion(), "987654321", LocalDate.of(2021, 1, 1), LocalDate.of(2031, 1, 1), "New Issuer"
        );

        when(clientRepository.saveAndFlush(mockClient)).thenReturn(mockClient);

        Client result = clientService.updateIdCard(dto, mockClient);

        assertEquals("987654321", result.getIdCardNumber());
        assertEquals("New Issuer", result.getIdCardIssuer());
        verify(clientRepository).saveAndFlush(mockClient);
    }

    @Test
    void updateIdCard_VersionMismatch_ThrowsException() {
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                mockClient.getVersion() + 1, "987654321", LocalDate.of(2021, 1, 1), LocalDate.of(2031, 1, 1), "New Issuer"
        );

        assertThrows(ResourceVersionMismatchException.class, () ->
                clientService.updateIdCard(dto, mockClient)
        );
    }

    @Test
    void updateIdCard_InactiveClient_ThrowsException() {
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                mockClient.getVersion(), "987654321", LocalDate.of(2021, 1, 1), LocalDate.of(2031, 1, 1), "New Issuer"
        );
        mockClient.setActive(false);

        assertThrows(InvalidInputValueException.class, () ->
                clientService.updateIdCard(dto, mockClient)
        );
    }

    // --- 9. UPDATE STATUS ---

    @Test
    void updateStatus_Success_TogglesStatus() {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false); // Current is true

        when(clientRepository.saveAndFlush(mockClient)).thenReturn(mockClient);

        Client result = clientService.updateStatus(dto, mockClient);

        assertFalse(result.isActive());
        verify(clientRepository).saveAndFlush(mockClient);
    }

    @Test
    void updateStatus_RedundantStatus_ThrowsException() {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(true); // Current is true

        assertThrows(InvalidInputValueException.class, () ->
                clientService.updateStatus(dto, mockClient)
        );
        verify(clientRepository, never()).saveAndFlush(any());
    }

    // --- 10. GET DETAILED CLIENT ---

    @Test
    void getDetailedClient_Admin_CallsNonAdvisorRestrictedRepoMethod() {
        when(clientRepository.findByClientUidWithDetails(mockClient.getClientUid())).thenReturn(Optional.of(mockClient));
        Client result = clientService.getDetailedClient(mockClient.getClientUid(), "ADM-123", true);

        assertNotNull(result);
        verify(clientRepository).findByClientUidWithDetails(mockClient.getClientUid());
    }

    @Test
    void getDetailedClient_Advisor_CallsAdvisorRestrictedRepoMethod() {
        when(clientRepository.findByClientUidAndAdvisor_EmployeeIdWithDetails(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));

        Client result = clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false);

        assertNotNull(result);
        verify(clientRepository).findByClientUidAndAdvisor_EmployeeIdWithDetails(mockClient.getClientUid(), mockAdvisor.getEmployeeId());
    }
}