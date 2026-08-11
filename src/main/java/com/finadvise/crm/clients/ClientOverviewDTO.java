package com.finadvise.crm.clients;

public record ClientOverviewDTO(
        String clientUid,
        String firstName,
        String lastName,
        String occupation,
        ClientStatisticsDTO statistics
) {}