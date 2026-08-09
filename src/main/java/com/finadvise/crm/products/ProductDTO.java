package com.finadvise.crm.products;

import com.finadvise.crm.clients.ClientSummaryDTO;
import com.finadvise.crm.users.AdvisorSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductDTO(
        Long id,
        String name,
        BigDecimal amount,
        LocalDate startDate,
        LocalDate endDate,
        ProductTypeDTO type,
        ProductProviderDTO provider,
        AdvisorSummaryDTO advisor,
        ClientSummaryDTO client
) {}