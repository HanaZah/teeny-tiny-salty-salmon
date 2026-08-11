package com.finadvise.crm.clients;

public record ClientStatisticsDTO(
        Long activeProducts,
        Long cashFlow
) {}