package com.example.ratelimiting.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class JwtIssuer {
    private final JwksService jwksService;

    public JwtIssuer(JwksService jwksService) {
        this.jwksService = jwksService;
    }

    public String issue(String subject, String role, long expiresInSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiresInSeconds);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("role", role)
                .build();

        RSAKey signingKey = jwksService.signingKey();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claims
        );
        try {
            jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
        return jwt.serialize();
    }
}

