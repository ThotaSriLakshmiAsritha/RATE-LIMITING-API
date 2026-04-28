package com.example.ratelimiting.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
        @Bean
        CorsConfigurationSource corsConfigurationSource(
                        @Value("${rate-limiting.cors.allowed-origins}") List<String> allowedOrigins
        ) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(allowedOrigins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "X-Correlation-Id"));
                configuration.setExposedHeaders(List.of("X-Correlation-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After"));
                configuration.setAllowCredentials(false);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

    @Bean
    @Profile("demo")
    @SuppressWarnings("unused")
    SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitingFilter rateLimitingFilter) throws Exception {
        return http
                                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/.well-known/jwks.json", "/api/auth/login").permitAll()
                        .requestMatchers("/ping").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(rateLimitingFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Profile("local")
    @SuppressWarnings("unused")
    SecurityFilterChain securityFilterChainLocal(HttpSecurity http, RateLimitingFilter rateLimitingFilter) throws Exception {
        return http
                                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/ping").permitAll()
                        .anyRequest().authenticated()
                )
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(rateLimitingFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .build();
    }

        private static JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                        Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
                        authorities.addAll(extractRoleAuthorities(jwt));
                        return authorities;
                });
                return converter;
        }

        private static List<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                addRoleAuthority(authorities, jwt.getClaimAsString("role"));

                List<String> roles = jwt.getClaimAsStringList("roles");
                if (roles != null) {
                        for (String role : roles) {
                                addRoleAuthority(authorities, role);
                        }
                }
                return authorities;
        }

        private static void addRoleAuthority(List<GrantedAuthority> authorities, String role) {
                if (role == null || role.isBlank()) {
                        return;
                }
                String normalized = role.toUpperCase(Locale.ROOT);
                if (!normalized.startsWith("ROLE_")) {
                        normalized = "ROLE_" + normalized;
                }
                authorities.add(new SimpleGrantedAuthority(normalized));
        }
}

