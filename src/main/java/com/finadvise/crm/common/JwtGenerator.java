package com.finadvise.crm.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtGenerator {

    @Value( "${SELF}")
    private String SELF;
    private final JwtEncoder jwtEncoder;

    public String generateToken(
            String subject, String scope, List<String> audience, Integer expirationInMinutes,
            Map<String, Object> extraClaims) {

        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationInMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SELF)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(subject)
                .audience(audience)
                .claim("scope", scope)
                .claims(existingClaimsMap -> {
                    if (extraClaims != null && !extraClaims.isEmpty()) {
                        existingClaimsMap.putAll(extraClaims);
                    }
                })
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String generateToken(
            String subject, String scope, List<String> audience, Integer expirationInMinutes) {
        return generateToken(subject, scope, audience, expirationInMinutes, null);
    }
}
