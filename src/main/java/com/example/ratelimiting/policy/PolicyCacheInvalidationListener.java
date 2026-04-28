package com.example.ratelimiting.policy;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PolicyCacheInvalidationListener implements MessageListener {
    private final PolicyService policyService;

    public PolicyCacheInvalidationListener(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        policyService.handleInvalidation(payload);
    }
}
