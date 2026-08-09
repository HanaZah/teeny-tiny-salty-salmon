package com.finadvise.crm.clients;

import java.util.Optional;

public interface ClientReadFacade {
    ClientSummaryDTO mapToClientSummary(Client client);
    Optional<Client> findByClientUidAndAdvisorEmployeeId(String clientUid, String employeeId);
}
