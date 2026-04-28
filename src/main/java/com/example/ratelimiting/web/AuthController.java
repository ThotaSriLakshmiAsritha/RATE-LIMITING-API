package com.example.ratelimiting.web;

import com.example.ratelimiting.persistence.UserEntity;
import com.example.ratelimiting.persistence.UserRepository;
import com.example.ratelimiting.ratelimit.annotation.RateLimited;
import com.example.ratelimiting.security.JwtIssuer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile("demo")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @RateLimited(requestsPerMinute = 10, burstCapacity = 3, dimension = com.example.ratelimiting.config.RateLimitingProperties.Dimension.IP,
            errorMessage = "Too many login attempts. Please try again later.")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        UserEntity user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        long expiresIn = 3600;
        String token = jwtIssuer.issue(user.getId().toString(), user.getRole(), expiresIn);
        return Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", expiresIn
        );
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}

