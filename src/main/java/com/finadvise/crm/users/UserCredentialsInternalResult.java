package com.finadvise.crm.users;

public record UserCredentialsInternalResult(
        UserDetailDTO userDetail,
        String rawPassword
) {}
