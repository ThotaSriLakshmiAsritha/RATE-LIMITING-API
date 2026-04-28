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
            Instant now = Instant.now();
            UserEntity user = userRepository.findByUsername("demo").orElseGet(() -> {
                UserEntity entity = new UserEntity();
                entity.setId(UUID.randomUUID());
                entity.setCreatedAt(now);
                return entity;
            });

            user.setUsername("demo");
            user.setEmail("demo@example.com");
            user.setPasswordHash(passwordEncoder.encode("password"));
            user.setRole("USER");
            user.setUpdatedAt(now);
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(now);
            }

            userRepository.save(user);
        };
    }
}

