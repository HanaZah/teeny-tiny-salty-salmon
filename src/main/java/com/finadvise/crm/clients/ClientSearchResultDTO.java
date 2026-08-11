package com.finadvise.crm.clients;

public record ClientSearchResultDTO(
        String clientUid,
        String name,
        String personalId,
        String cityName,
        String statusLabel
) {}