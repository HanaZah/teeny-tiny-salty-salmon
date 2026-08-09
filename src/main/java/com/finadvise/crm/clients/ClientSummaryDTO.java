package com.finadvise.crm.clients;

public record ClientSummaryDTO(
        String clientUid,
        String firstName,
        String lastName
) {}