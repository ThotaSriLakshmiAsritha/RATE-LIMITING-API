package com.example.ratelimiting.policy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.util.Objects;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;

@Configuration
@EnableConfigurationProperties(PolicyCacheProperties.class)
public class PolicyCacheConfig {
    @Bean
    @ConditionalOnProperty(
            name = "rate-limiting.policy-cache.redis-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
        @SuppressWarnings("unused")
        RedisMessageListenerContainer policyInvalidationListenerContainer(
            @NonNull RedisConnectionFactory connectionFactory,
            @NonNull PolicyCacheProperties properties,
            @NonNull PolicyCacheInvalidationListener listener
        ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        String channel = Objects.requireNonNull(properties.getInvalidationChannel());
        container.addMessageListener(listener, new ChannelTopic(channel));
        return container;
    }
}
