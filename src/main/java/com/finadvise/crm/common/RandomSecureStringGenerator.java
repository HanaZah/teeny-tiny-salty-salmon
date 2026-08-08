package com.finadvise.crm.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class RandomSecureStringGenerator {
    static final String passwordCharPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    SecureRandom random = new SecureRandom();

    public String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(passwordCharPool.length());
            sb.append(passwordCharPool.charAt(randomIndex));
        }
        return sb.toString();
    }

    public String generateRandomNumeric(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(10);
            sb.append(randomIndex);
        }
        return sb.toString();
    }

}
