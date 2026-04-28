package com.example.ratelimiting.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class JwksService {
    private final RSAKey rsaJwk;

    public JwksService() {
        this.rsaJwk = generate();
    }

    public JWKSet publicJwkSet() {
        return new JWKSet(rsaJwk.toPublicJWK());
    }

    public RSAKey signingKey() {
        return rsaJwk;
    }

    private static RSAKey generate() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();
            return new RSAKey.Builder(pub)
                    .privateKey(priv)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key", e);
        }
    }
}

