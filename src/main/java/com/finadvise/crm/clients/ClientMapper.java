package com.finadvise.crm.clients;

import org.springframework.stereotype.Component;

@Component
class ClientMapper {
    ClientSummaryDTO toSummaryDTO(Client client) {
        if (client == null) {
            return null;
        }

        return new ClientSummaryDTO(
                client.getClientUid(),
                client.getFirstName(),
                client.getLastName()
        );
    }
}
