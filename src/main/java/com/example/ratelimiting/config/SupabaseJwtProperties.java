package com.example.ratelimiting.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@Profile("local")
@ConfigurationProperties(prefix = "rate-limiting.supabase")
public class SupabaseJwtProperties {
    @NotBlank
    private String issuer;

    @NotBlank
    private String audience;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}

