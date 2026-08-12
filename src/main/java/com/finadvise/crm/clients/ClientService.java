package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.common.*;
import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;
import com.finadvise.crm.users.User;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class ClientService implements ClientReadFacade {
    private final ClientRepository clientRepository;
    private final ClientSearchMinimalRepository clientSearchMinimalRepository;
    private final ClientMapper clientMapper;
    private final ObfuscatedIdGenerator obfuscatedIdGenerator;
    private final Clock clock;

    @Override
    public ClientSummaryDTO mapToClientSummary(Client client) {
        return clientMapper.toSummaryDTO(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findByClientUidAndAdvisorEmployeeId(String clientUid, String employeeId) {
        return clientRepository.findByClientUidAndAdvisor_EmployeeId(clientUid, employeeId);
    }

    @Override
    public List<StaticDictionaryItemDTO> getAllClientStates() {
        return Arrays.stream(ClientStatus.values()).map(clientMapper::toStaticDictionaryItemDto).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADVISOR') and #employeeId == authentication.name")
    public List<ClientOverviewDTO> getRecentClientOverviews(String employeeId, int limit) {
        if (limit <= 0 || limit > 20) {
            throw new InvalidInputValueException("Invalid limit value, must be between 1 and 20");
        }

        return clientRepository.findRecentClientOverviews(employeeId, LocalDate.now(clock), Limit.of(limit))
                .stream()
                .map(clientMapper::toOverviewDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN') and #employeeId == authentication.name" +
            " and (hasAuthority('ADMIN') == #isAdmin)")
    public List<ClientSuggestionResultDTO> getClientSuggestions(
            ClientSuggestionRequestDTO request, String employeeId, boolean isAdmin) {

        String normalizedName = request.name() != null
                ? request.name().trim().toLowerCase()
                : "";

        if (isAdmin) {
            return clientRepository.findClientSuggestions(normalizedName, request.limit());
        } else {
            return clientRepository.findClientSuggestionsForAdvisor(normalizedName, employeeId, request.limit());
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN') and #employeeId == authentication.name" +
            " and (hasAuthority('ADMIN') == #isAdmin)")
    public Client getDetailedClient(String clientUid, String employeeId, boolean isAdmin) {
        if (isAdmin) {
            return clientRepository.findByClientUidWithDetails(clientUid)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found or access denied"));
        } else {
            return clientRepository.findByClientUidAndAdvisor_EmployeeIdWithDetails(clientUid, employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found or access denied"));
        }
    }

    @Transactional(readOnly = true)
    public Optional<Client> getDetailedClientByClientUidAndEmployeeId(String clientUid, String employeeId) {
        return clientRepository.findByClientUidAndAdvisor_EmployeeIdWithDetails(clientUid, employeeId);
    }

    @PreAuthorize("hasAnyAuthority('ADVISOR', 'ADMIN') and #employeeId == authentication.name" +
            " and (hasAuthority('ADMIN') == #isAdmin)")
    @Transactional(readOnly = true)
    public Page<ClientSearchResultDTO> searchClients(
            ClientSearchCriteriaDTO criteria, Pageable pageable, String employeeId, boolean isAdmin) {

        ClientSearchCriteriaDTO secureCriteria = criteria;

        if (!isAdmin) {
            secureCriteria = criteria.withEmployeeId(employeeId);
        }

        Specification<ClientSearchMinimal> specification = ClientSearchMinimalSpecification.build(secureCriteria);

        return clientSearchMinimalRepository.findAll(specification, pageable)
                .map(clientMapper::toSearchResultDTO);
    }

    @Transactional(readOnly = true)
    public void validateClientIdCard(
            @NonNull LocalDate idCardExpiryDate,
            @NonNull LocalDate idCardIssueDate,
            @NonNull LocalDate birthDate,
            @NonNull String idCardNumber,
            @Nullable String currentIdCardNumber) {

        if (!Objects.equals(idCardNumber, currentIdCardNumber) && clientRepository.existsByIdCardNumber(idCardNumber)) {
            throw new ResourceConflictException("Client with this ID card number already exists");
        }

        if (!idCardIssueDate.isAfter(birthDate)) {
            throw new InvalidInputValueException("Client ID card issue must be after the birth date");
        }

        if (idCardIssueDate.isAfter(LocalDate.now(clock))) {
            throw new InvalidInputValueException("ID card issue date cannot be in the future");
        }

        if (idCardIssueDate.isAfter(idCardExpiryDate)) {
            throw new InvalidInputValueException("ID card issue date must be before expiry date");
        }

        if (idCardExpiryDate.isBefore(LocalDate.now(clock))) {
            throw new InvalidInputValueException("Cannot create client with expired ID card");
        }
    }

    @Transactional(readOnly = true)
    public void validateGeneralInfo(
            @NonNull LocalDate birthDate,
            @NonNull String personalId,
            @Nullable String currentPersonalId) {

        final int LEGAL_AGE_LIMIT = 18;

        if (!Objects.equals(personalId, currentPersonalId) && clientRepository.existsByPersonalId(personalId)) {
            throw new ResourceConflictException("Client with this personal ID already exists");
        }

        if (Period.between(birthDate, LocalDate.now(clock)).getYears() < LEGAL_AGE_LIMIT) {
            throw new InvalidInputValueException("Client must be at least " + LEGAL_AGE_LIMIT + " years old");
        }
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADVISOR') and #advisor.employeeId == authentication.name")
    public Client createClient(ClientCreateDTO dto, User advisor, Address residentialAddress, Address contactAddress) {
        Long id = clientRepository.getNextSequenceValue();
        String clientUid = obfuscatedIdGenerator.encode(id);

        Client client = Client.builder()
                .id(id)
                .clientUid(clientUid)
                .personalId(dto.personalId())
                .birthDate(dto.birthDate())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .occupation(dto.occupation())
                .phone(dto.phone())
                .email(dto.email())
                .idCardNumber(dto.idCardNumber())
                .idCardIssueDate(dto.idCardIssueDate())
                .idCardExpiryDate(dto.idCardExpiryDate())
                .idCardIssuer(dto.idCardIssuer())
                .advisor(advisor)
                .residentialAddress(residentialAddress)
                .contactAddress(contactAddress)
                .build();

        return clientRepository.saveAndFlush(client);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADVISOR') and #client.advisor.employeeId == authentication.name")
    public Client updateGeneralInfo(
            ClientGeneralUpdateDTO dto,
            Client client,
            Address residentialAddress,
            Address contactAddress) {

        if (!client.getVersion().equals(dto.version())) {
            throw new ResourceVersionMismatchException("Client record has been updated since last read. " +
                    "Please refresh and retry.");
        }

        client.validateEligibilityForUpdate();

        client.setPersonalId(dto.personalId());
        client.setBirthDate(dto.birthDate());
        client.setFirstName(dto.firstName());
        client.setLastName(dto.lastName());
        client.setOccupation(dto.occupation());
        client.setPhone(dto.phone());
        client.setEmail(dto.email());

        if (residentialAddress != null) {
            client.setResidentialAddress(residentialAddress);
        }
        if (contactAddress != null) {
            client.setContactAddress(contactAddress);
        }

        return clientRepository.saveAndFlush(client);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADVISOR') and #client.advisor.employeeId == authentication.name")
    public Client updateIdCard(ClientIdCardUpdateDTO dto, Client client) {
        if (!client.getVersion().equals(dto.version())) {
            throw new ResourceVersionMismatchException("Client record has been updated since last read. " +
                    "Please refresh and retry.");
        }

        client.validateEligibilityForUpdate();

        client.setIdCardNumber(dto.idCardNumber());
        client.setIdCardIssueDate(dto.idCardIssueDate());
        client.setIdCardExpiryDate(dto.idCardExpiryDate());
        client.setIdCardIssuer(dto.idCardIssuer());

        return clientRepository.saveAndFlush(client);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public Client updateStatus(ClientStatusUpdateDTO dto, Client client) {
        if (client.isActive() == dto.isActive()) {
            throw new InvalidInputValueException("Client is already in the desired state.");
        }

        client.setActive(dto.isActive());
        return clientRepository.saveAndFlush(client);
    }
}
