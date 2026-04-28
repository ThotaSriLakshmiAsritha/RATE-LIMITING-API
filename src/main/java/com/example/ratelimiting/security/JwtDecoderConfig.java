package com.example.ratelimiting.security;

import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile("demo")
public class JwtDecoderConfig {
    @Bean
    @SuppressWarnings("unused")
    JwtDecoder localJwtDecoder(JwksService jwksService) {
        RSAPublicKey publicKey;
        try {
            publicKey = jwksService.signingKey().toRSAPublicKey();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build local JwtDecoder", e);
        }
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}

