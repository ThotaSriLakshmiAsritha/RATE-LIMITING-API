package com.example.ratelimiting.dashboard;

import com.example.ratelimiting.config.RateLimitingProperties;
import com.example.ratelimiting.policy.PolicyMode;
import com.example.ratelimiting.policy.PolicyScope;
import com.example.ratelimiting.policy.PolicySnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class DashboardPolicyDtos {
    private DashboardPolicyDtos() {
    }

    public record PolicyResponse(
            UUID id,
            String tenantId,
            PolicyScope scopeType,
            String scopeId,
            String endpointPattern,
            Integer requestsPerMinute,
            Integer burstCapacity,
            RateLimitingProperties.Dimension dimension,
            String errorMessage,
            PolicyMode mode,
            Integer priority,
            long version
    ) {
        public static PolicyResponse fromSnapshot(PolicySnapshot snapshot) {
            return new PolicyResponse(
                    snapshot.id(),
                    snapshot.tenantId(),
                    snapshot.scopeType(),
                    snapshot.scopeId(),
                    snapshot.endpointPattern(),
                    snapshot.requestsPerMinute(),
                    snapshot.burstCapacity(),
                    snapshot.dimension(),
                    snapshot.errorMessage(),
                    snapshot.mode(),
                    snapshot.priority(),
                    snapshot.version()
            );
        }
    }

    public record UpdatePolicyRequest(
            @NotBlank String tenantId,
            @NotNull PolicyScope scopeType,
            String scopeId,
            String endpointPattern,
            Integer requestsPerMinute,
            Integer burstCapacity,
            RateLimitingProperties.Dimension dimension,
            String errorMessage,
            PolicyMode mode,
            Integer priority,
            Boolean enabled
    ) {
    }
}
