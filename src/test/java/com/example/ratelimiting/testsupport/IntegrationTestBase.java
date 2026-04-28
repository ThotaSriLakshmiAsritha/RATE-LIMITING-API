package com.example.ratelimiting.testsupport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {
    @BeforeAll
    static void startContainers() {
        boolean dockerAvailable;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            dockerAvailable = false;
        }
        Assumptions.assumeTrue(dockerAvailable, "Docker is required for Testcontainers integration tests");

        if (!Containers.POSTGRES.isRunning()) {
            Containers.POSTGRES.start();
        }
        if (!Containers.REDIS.isRunning()) {
            Containers.REDIS.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.profiles.active", () -> "demo");
        registry.add("spring.datasource.url", Containers.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", Containers.POSTGRES::getUsername);
        registry.add("spring.datasource.password", Containers.POSTGRES::getPassword);

        registry.add("spring.data.redis.host", Containers.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> Containers.REDIS.getMappedPort(6379));
    }
}

