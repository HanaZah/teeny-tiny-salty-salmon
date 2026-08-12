package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressDTO;
import com.finadvise.crm.budget.FullBudgetDTO;
import com.finadvise.crm.dictionaries.StaticDictionaryItemDTO;
import com.finadvise.crm.users.AdvisorSummaryDTO;
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

    ClientDetailDTO toDetailDTO(
            Client client,
            AdvisorSummaryDTO advisor,
            AddressDTO permanentAddress,
            AddressDTO contactAddress,
            FullBudgetDTO budget) {
        // all fields are mandatory
        if (client == null || advisor == null || permanentAddress == null || contactAddress == null || budget == null) {
            return null;
        }

        return new ClientDetailDTO(
                client.getVersion(),
                client.getClientUid(),
                client.getFirstName(),
                client.getLastName(),
                client.getPersonalId(),
                client.getBirthDate(),
                client.getOccupation(),
                client.getPhone(),
                client.getEmail(),
                client.getIdCardNumber(),
                client.getIdCardIssuer(),
                client.getIdCardIssueDate(),
                client.getIdCardExpiryDate(),
                client.getLastUpdate(),
                client.isActive(),
                advisor,
                permanentAddress,
                contactAddress,
                budget
        );
    }

    ClientOverviewDTO toOverviewDTO(ClientOverviewProjection projection) {
        if (projection == null) {
            return null;
        }

        return new ClientOverviewDTO(
                projection.getClientUid(),
                projection.getFirstName(),
                projection.getLastName(),
                projection.getOccupation(),
                new ClientStatisticsDTO(
                        projection.getActiveProducts(),
                        projection.getTotalIncome() - projection.getTotalExpense()
                )
        );
    }

    ClientSearchResultDTO toSearchResultDTO(ClientSearchMinimal client) {
        if (client == null) {
            return null;
        }

        return new ClientSearchResultDTO(
                client.getClientUid(),
                client.getFullName(),
                client.getPersonalId(),
                client.getContactCityName(),
                client.isActive()? ClientStatus.ACTIVE.getLabel() : ClientStatus.INACTIVE.getLabel()
        );
    }

    StaticDictionaryItemDTO toStaticDictionaryItemDto(ClientStatus status) {
        if (status == null) {
            return null;
        }
        return new StaticDictionaryItemDTO(status.name(), status.getLabel());
    }
}
