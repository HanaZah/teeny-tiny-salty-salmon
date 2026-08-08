package com.finadvise.crm.users;

public record AuthResponseDTO(
        String accessToken,
        String tokenType
) {}
