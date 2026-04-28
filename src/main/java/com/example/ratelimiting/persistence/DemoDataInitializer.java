package com.example.ratelimiting.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner seedDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepository.findByUsername("demo").orElseGet(() -> {
                UserEntity u = new UserEntity();
                u.setId(UUID.randomUUID());
                u.setUsername("demo");
                u.setEmail("demo@example.com");
                u.setPasswordHash(passwordEncoder.encode("password"));
                u.setRole("USER");
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return userRepository.save(u);
            });
        };
    }
}

