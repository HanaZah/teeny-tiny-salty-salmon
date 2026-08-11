package com.finadvise.crm.clients;

import com.finadvise.crm.TestFixtureFactory;
import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.addresses.AddressDTO;
import com.finadvise.crm.addresses.AddressFacade;
import com.finadvise.crm.addresses.AddressInputDTO;
import com.finadvise.crm.budget.BudgetReadFacade;
import com.finadvise.crm.budget.FullBudgetDTO;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.common.SystemIntegrityException;
import com.finadvise.crm.users.AdvisorSummaryDTO;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserReadFacade;
import com.finadvise.crm.users.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientDetailOrchestratorTest {

    @Mock private UserReadFacade userReadFacade;
    @Mock private AddressFacade addressFacade;
    @Mock private BudgetReadFacade budgetReadFacade;
    @Mock private ClientMapper clientMapper;
    @Mock private ClientService clientService;

    @InjectMocks
    private ClientDetailOrchestrator orchestrator;

    private User mockAdvisor;
    private Client mockClient;
    private Address mockAddress;
    private ClientDetailDTO mockDetailDTO;

    @BeforeEach
    void setUp() {
        mockAdvisor = TestFixtureFactory.createValidUser("EMP-123", UserType.ADVISOR);
        mockClient = TestFixtureFactory.createValidClient(1L, "C-123456", mockAdvisor);
        mockAddress = mockClient.getResidentialAddress(); // Reusing the valid dummy address

        mockDetailDTO = new ClientDetailDTO(
                0, "C-123456", "John", "Smith", "9001011234", LocalDate.of(1990, 1, 1),
                "Software Engineer", "+420111222333", "client1@example.com",
                "123456789", "MV CR", LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1),
                LocalDate.now(), true,
                new AdvisorSummaryDTO("EMP-123", "Jane", "Doe"),
                new AddressDTO(1L, "Test Street", "123/A", "Test City", "123 45"),
                new AddressDTO(1L, "Test Street", "123/A", "Test City", "123 45"),
                new FullBudgetDTO(List.of(), List.of(), 0)
        );
    }

    // --- 1. GET CLIENT DETAIL ---

    @Test
    void getClientDetail_OrchestrationSuccess() {
        when(clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false))
                .thenReturn(mockClient);

        when(userReadFacade.mapToAdvisorSummary(mockAdvisor)).thenReturn(mockDetailDTO.advisor());
        when(addressFacade.mapToDto(mockClient.getResidentialAddress())).thenReturn(mockDetailDTO.permanentAddress());
        when(addressFacade.mapToDto(mockClient.getContactAddress())).thenReturn(mockDetailDTO.contactAddress());
        when(budgetReadFacade.getFullBudgetForClient(mockClient.getClientUid())).thenReturn(mockDetailDTO.budget());

        when(clientMapper.toDetailDTO(
                eq(mockClient), eq(mockDetailDTO.advisor()), eq(mockDetailDTO.permanentAddress()),
                eq(mockDetailDTO.contactAddress()), eq(mockDetailDTO.budget())
        )).thenReturn(mockDetailDTO);

        ClientDetailDTO result = orchestrator.getClientDetail(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false);

        assertNotNull(result);
        assertEquals(mockDetailDTO, result);
        verify(clientService).getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false);
    }

    // --- 2. CREATE CLIENT ---

    @Test
    void createClient_OrchestrationSuccess_ValidatesBeforeDatabaseOps() {
        AddressInputDTO addressInput = new AddressInputDTO("Test Street", "123/A", "Test City", "123 45");
        ClientCreateDTO createDTO = new ClientCreateDTO(
                "9001011234", LocalDate.of(1990, 1, 1), "John", "Smith", "IT", "+420111", "a@b.c",
                "123456789", LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), "MVCR",
                addressInput, addressInput
        );

        AddressDTO mockAddressDTO = new AddressDTO(1L, "Test Street", "123/A", "Test City", "123 45");

        when(addressFacade.findOrCreateAddress(addressInput)).thenReturn(mockAddressDTO);
        when(addressFacade.getReferenceById(1L)).thenReturn(mockAddress);
        when(userReadFacade.findByEmployeeId(mockAdvisor.getEmployeeId())).thenReturn(Optional.of(mockAdvisor));
        when(clientService.createClient(createDTO, mockAdvisor, mockAddress, mockAddress)).thenReturn(mockClient);

        // Mock the internal getClientDetail call
        when(clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false)).thenReturn(mockClient);
        when(clientMapper.toDetailDTO(any(), any(), any(), any(), any())).thenReturn(mockDetailDTO);

        ClientDetailDTO result = orchestrator.createClient(createDTO, mockAdvisor.getEmployeeId());

        assertNotNull(result);
        assertEquals(mockDetailDTO, result);

        // Verify sequence: Validations MUST occur before address operations
        InOrder inOrder = inOrder(clientService, addressFacade);
        inOrder.verify(clientService).validateGeneralInfo(createDTO.birthDate(), createDTO.personalId(), null);
        inOrder.verify(clientService).validateClientIdCard(createDTO.idCardExpiryDate(), createDTO.idCardIssueDate(), createDTO.birthDate(), createDTO.idCardNumber(), null);
        inOrder.verify(addressFacade, times(2)).findOrCreateAddress(addressInput);
    }

    @Test
    void createClient_MissingAdvisor_ThrowsSystemIntegrityException() {
        ClientCreateDTO createDTO = new ClientCreateDTO(
                "9001011234", LocalDate.of(1990, 1, 1), "John", "Smith", "IT", "+420111", "a@b.c",
                "123456789", LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), "MVCR",
                new AddressInputDTO("S", "1", "C", "111 00"), new AddressInputDTO("S", "1", "C", "111 00")
        );

        when(addressFacade.findOrCreateAddress(any())).thenReturn(new AddressDTO(1L, "S", "1", "C", "111 00"));
        when(addressFacade.getReferenceById(any())).thenReturn(mockAddress);
        when(userReadFacade.findByEmployeeId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(SystemIntegrityException.class, () ->
                orchestrator.createClient(createDTO, "UNKNOWN")
        );

        verify(clientService, never()).createClient(any(), any(), any(), any());
    }

    // --- 3. UPDATE CLIENT GENERAL INFO ---

    @Test
    void updateClientGeneralInfo_AddressesUnchanged_Success() {
        // Values identical to mockAddress fields
        AddressInputDTO identicalAddress = new AddressInputDTO(
                mockAddress.getStreet(), mockAddress.getHouseNumber(), mockAddress.getCity(), mockAddress.getPostalCode()
        );
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                mockClient.getVersion(), "9001011234", LocalDate.of(1990, 1, 1), "John", "Smith",
                "IT", "+420111", "a@b.c", identicalAddress, identicalAddress
        );

        when(clientService.getDetailedClientByClientUidAndEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(clientService.updateGeneralInfo(dto, mockClient, null, null)).thenReturn(mockClient);

        // Mock internal getClientDetail
        when(clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false)).thenReturn(mockClient);
        when(clientMapper.toDetailDTO(any(), any(), any(), any(), any())).thenReturn(mockDetailDTO);

        ClientDetailDTO result = orchestrator.updateClientGeneralInfo(dto, mockClient.getClientUid(), mockAdvisor.getEmployeeId());

        assertNotNull(result);

        // Ensure addresses were NOT resolved via DB
        verify(addressFacade, never()).findOrCreateAddress(any());
        verify(addressFacade, never()).getReferenceById(anyLong());

        // Ensure nulls were passed to service
        verify(clientService).updateGeneralInfo(dto, mockClient, null, null);
    }

    @Test
    void updateClientGeneralInfo_AddressesChanged_Success() {
        AddressInputDTO changedAddress = new AddressInputDTO("New Street", "99", "New City", "999 00");
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                mockClient.getVersion(), "9001011234", LocalDate.of(1990, 1, 1), "John", "Smith",
                "IT", "+420111", "a@b.c", changedAddress, changedAddress
        );

        AddressDTO mockNewAddressDTO = new AddressDTO(2L, "New Street", "99", "New City", "999 00");
        Address mockNewAddress = Address.builder().id(2L).street("New Street").build();

        when(clientService.getDetailedClientByClientUidAndEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(addressFacade.findOrCreateAddress(changedAddress)).thenReturn(mockNewAddressDTO);
        when(addressFacade.getReferenceById(2L)).thenReturn(mockNewAddress);

        when(clientService.updateGeneralInfo(dto, mockClient, mockNewAddress, mockNewAddress)).thenReturn(mockClient);

        // Mock internal getClientDetail
        when(clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false)).thenReturn(mockClient);
        when(clientMapper.toDetailDTO(any(), any(), any(), any(), any())).thenReturn(mockDetailDTO);

        ClientDetailDTO result = orchestrator.updateClientGeneralInfo(dto, mockClient.getClientUid(), mockAdvisor.getEmployeeId());

        assertNotNull(result);

        // Ensure address resolution logic was triggered
        verify(addressFacade, times(2)).findOrCreateAddress(changedAddress);
        verify(addressFacade, times(2)).getReferenceById(2L);
        verify(clientService).updateGeneralInfo(dto, mockClient, mockNewAddress, mockNewAddress);
    }

    @Test
    void updateClientGeneralInfo_NotFound_ThrowsException() {
        ClientGeneralUpdateDTO dto = new ClientGeneralUpdateDTO(
                0, "9001011234", LocalDate.now(), "A", "B", "C", "+1", "a@b", null, null
        );

        when(clientService.getDetailedClientByClientUidAndEmployeeId("UNKNOWN", mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orchestrator.updateClientGeneralInfo(dto, "UNKNOWN", mockAdvisor.getEmployeeId())
        );
    }

    // --- 4. UPDATE CLIENT ID CARD ---

    @Test
    void updateClientIdCard_OrchestrationSuccess() {
        ClientIdCardUpdateDTO dto = new ClientIdCardUpdateDTO(
                mockClient.getVersion(), "999888777", LocalDate.of(2021, 1, 1), LocalDate.of(2031, 1, 1), "New Issuer"
        );

        when(clientService.getDetailedClientByClientUidAndEmployeeId(mockClient.getClientUid(), mockAdvisor.getEmployeeId()))
                .thenReturn(Optional.of(mockClient));
        when(clientService.updateIdCard(dto, mockClient)).thenReturn(mockClient);

        // Mock internal getClientDetail
        when(clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), false)).thenReturn(mockClient);
        when(clientMapper.toDetailDTO(any(), any(), any(), any(), any())).thenReturn(mockDetailDTO);

        ClientDetailDTO result = orchestrator.updateClientIdCard(dto, mockClient.getClientUid(), mockAdvisor.getEmployeeId());

        assertNotNull(result);
        verify(clientService).validateClientIdCard(
                dto.idCardExpiryDate(), dto.idCardIssueDate(), mockClient.getBirthDate(), dto.idCardNumber(), mockClient.getIdCardNumber()
        );
        verify(clientService).updateIdCard(dto, mockClient);
    }

    // --- 5. UPDATE CLIENT STATUS ---

    @Test
    void updateClientStatus_OrchestrationSuccess() {
        ClientStatusUpdateDTO dto = new ClientStatusUpdateDTO(false);

        // Uses admin retrieval
        when(clientService.getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), true))
                .thenReturn(mockClient);
        when(clientService.updateStatus(dto, mockClient)).thenReturn(mockClient);

        when(clientMapper.toDetailDTO(any(), any(), any(), any(), any())).thenReturn(mockDetailDTO);

        ClientDetailDTO result = orchestrator.updateClientStatus(dto, mockClient.getClientUid(), mockAdvisor.getEmployeeId());

        assertNotNull(result);
        verify(clientService).updateStatus(dto, mockClient);
        // Verify it calls getClientDetail internally with isAdmin = true (which then calls getDetailedClient with isAdmin = true)
        verify(clientService, times(2)).getDetailedClient(mockClient.getClientUid(), mockAdvisor.getEmployeeId(), true);
    }
}