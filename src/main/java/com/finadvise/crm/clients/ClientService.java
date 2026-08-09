package com.finadvise.crm.clients;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class ClientService implements ClientReadFacade {
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Override
    public ClientSummaryDTO mapToClientSummary(Client client) {
        return clientMapper.toSummaryDTO(client);
    }

    @Override
    public Optional<Client> findByClientUidAndAdvisorEmployeeId(String clientUid, String employeeId) {
        return clientRepository.findByClientUidAndAdvisor_EmployeeId(clientUid, employeeId);
    }
}
