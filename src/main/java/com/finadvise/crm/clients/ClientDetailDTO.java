package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressDTO;
import com.finadvise.crm.budget.FullBudgetDTO;
import com.finadvise.crm.products.ProductsStatisticsDTO;
import com.finadvise.crm.users.AdvisorSummaryDTO;

import java.time.LocalDate;

public record ClientDetailDTO(
        Integer version,
        String clientUid,
        String firstName,
        String lastName,
        String personalId,
        LocalDate birthDate,
        String occupation,
        String phone,
        String email,
        String idCardNumber,
        String idCardIssuer,
        LocalDate idCardIssueDate,
        LocalDate idCardExpiryDate,
        LocalDate lastUpdate,
        Boolean isActive,
        AdvisorSummaryDTO advisor,
        AddressDTO permanentAddress,
        AddressDTO contactAddress,
        FullBudgetDTO budget,
        ProductsStatisticsDTO productsStatistics
) {}