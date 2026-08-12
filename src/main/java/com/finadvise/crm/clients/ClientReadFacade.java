package com.finadvise.crm.clients;

import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;

import java.util.List;
import java.util.Optional;

public interface ClientReadFacade {
    ClientSummaryDTO mapToClientSummary(Client client);
    Optional<Client> findByClientUidAndAdvisorEmployeeId(String clientUid, String employeeId);
    List<StaticDictionaryItemDTO> getAllClientStates();
}
