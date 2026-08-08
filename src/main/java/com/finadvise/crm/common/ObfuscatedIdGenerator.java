package com.finadvise.crm.common;

import com.finadvise.crm.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hashids.Hashids;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObfuscatedIdGenerator {
    private final SecurityProperties securityProperties;
    private static final int DB_COLUMN_MAX = 20;
    private static final long HASHIDS_LIMIT = 9007199254740991L;
    private Hashids hashids;

    @PostConstruct
    public void init() {
        this.hashids = new Hashids(
                securityProperties.hashidSalt(),
                securityProperties.hashidLength(),
                securityProperties.hashidAlphabet()
        );

        // Sanity check: Test a very large ID to ensure it fits the column
        String testHash = hashids.encode(HASHIDS_LIMIT);

        if (testHash.length() > DB_COLUMN_MAX) {
            throw new IllegalStateException(
                    String.format("Hashid configuration exceeds DB column limit! Hash: %s, Max allowed: %s",
                            testHash.length(), DB_COLUMN_MAX)
            );
        }
    }

    public String encode(Long id) {
        return hashids.encode(id);
    }
}
