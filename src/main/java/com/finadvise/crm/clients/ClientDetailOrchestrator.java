package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.addresses.AddressFacade;
import com.finadvise.crm.budget.BudgetReadFacade;
import com.finadvise.crm.common.*;
import com.finadvise.crm.users.User;
import com.finadvise.crm.users.UserReadFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ClientDetailOrchestrator {
    private final UserReadFacade userReadFacade;
    private final AddressFacade addressFacade;
    private final BudgetReadFacade budgetReadFacade;
    private final ClientMapper clientMapper;
    private final ClientService clientService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN') and #employeeId == authentication.name" +
            " and (hasAuthority('ADMIN') == #isAdmin)")
    public ClientDetailDTO getClientDetail(String clientUid, String employeeId, boolean isAdmin) {
        Client client = clientService.getDetailedClient(clientUid, employeeId, isAdmin);

        return clientMapper.toDetailDTO(
                client,
                userReadFacade.mapToAdvisorSummary(client.getAdvisor()),
                addressFacade.mapToDto(client.getResidentialAddress()),
                addressFacade.mapToDto(client.getContactAddress()),
                budgetReadFacade.getFullBudgetForClient(clientUid)
        );
    }

    @PreAuthorize("hasAuthority('ADVISOR') and #employeeId == authentication.name")
    @Transactional
    public ClientDetailDTO createClient(ClientCreateDTO dto, String employeeId) {
        clientService.validateGeneralInfo(dto.birthDate(), dto.personalId(), null);
        clientService.validateClientIdCard(
                dto.idCardExpiryDate(),
                dto.idCardIssueDate(),
                dto.birthDate(),
                dto.idCardNumber(),
                null);

        Address residentialAddress = addressFacade.getReferenceById(
                addressFacade.findOrCreateAddress(dto.residentialAddress()).id()
        );
        Address contactAddress = addressFacade.getReferenceById(
                addressFacade.findOrCreateAddress(dto.contactAddress()).id()
        );

        User advisor = userReadFacade.findByEmployeeId(employeeId).orElseThrow(
                () -> new SystemIntegrityException("error.system.user-missing")
        );

        return getClientDetail(
                clientService.createClient(dto, advisor, residentialAddress, contactAddress).getClientUid(),
                advisor.getEmployeeId(),
                false);
    }

    @PreAuthorize("hasAuthority('ADVISOR') and #employeeId == authentication.name")
    @Transactional
    public ClientDetailDTO updateClientGeneralInfo(ClientGeneralUpdateDTO dto, String clientUid, String employeeId) {
        Client client = clientService.getDetailedClientByClientUidAndEmployeeId(clientUid, employeeId).orElseThrow(
                () -> new ResourceNotFoundException("error.client.not-found")
        );

        clientService.validateGeneralInfo(dto.birthDate(), dto.personalId(), client.getPersonalId());

        boolean isResidentialAddressChanged = !client.getResidentialAddress().matches(
                dto.residentialAddress().street(),
                dto.residentialAddress().houseNumber(),
                dto.residentialAddress().city(),
                dto.residentialAddress().postalCode()
        );

        boolean isContactAddressChanged = !client.getContactAddress().matches(
                dto.contactAddress().street(),
                dto.contactAddress().houseNumber(),
                dto.contactAddress().city(),
                dto.contactAddress().postalCode()
        );

        Address residentialAddress = null;
        Address contactAddress = null;

        if (isResidentialAddressChanged) {
            residentialAddress = addressFacade.getReferenceById(
                    addressFacade.findOrCreateAddress(dto.residentialAddress()).id()
            );
        }

        if (isContactAddressChanged) {
            contactAddress = addressFacade.getReferenceById(
                    addressFacade.findOrCreateAddress(dto.contactAddress()).id()
            );
        }

        return getClientDetail(
                clientService.updateGeneralInfo(dto, client, residentialAddress, contactAddress).getClientUid(),
                employeeId,
                false);
    }

    @PreAuthorize("hasAuthority('ADVISOR') and #employeeId == authentication.name")
    @Transactional
    public ClientDetailDTO updateClientIdCard(ClientIdCardUpdateDTO dto, String clientUid, String employeeId) {
        Client client = clientService.getDetailedClientByClientUidAndEmployeeId(clientUid, employeeId).orElseThrow(
                () -> new ResourceNotFoundException("error.client.not-found")
        );

        clientService.validateClientIdCard(
                dto.idCardExpiryDate(),
                dto.idCardIssueDate(),
                client.getBirthDate(),
                dto.idCardNumber(),
                client.getIdCardNumber());

        return getClientDetail(
                clientService.updateIdCard(dto,client).getClientUid(),
                employeeId,
                false
        );
    }

    @PreAuthorize("hasAuthority('ADMIN') and #employeeId == authentication.name")
    @Transactional
    public ClientDetailDTO updateClientStatus(ClientStatusUpdateDTO dto, String clientUid, String employeeId) {
        Client client = clientService.getDetailedClient(clientUid, employeeId, true);

        return getClientDetail(
                clientService.updateStatus(dto,client).getClientUid(),
                employeeId,
                true
        );
    }
}