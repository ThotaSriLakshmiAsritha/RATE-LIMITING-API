package com.example.ratelimiting.policy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {
    List<PolicyEntity> findByTenantIdOrderByPriorityDescUpdatedAtDesc(String tenantId);

    Optional<PolicyEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<PolicyEntity> findByTenantIdAndScopeTypeAndEnabledTrueOrderByPriorityDesc(String tenantId, PolicyScope scopeType);

    List<PolicyEntity> findByTenantIdAndScopeTypeAndScopeIdAndEnabledTrueOrderByPriorityDesc(
            String tenantId,
            PolicyScope scopeType,
            String scopeId
    );
}
