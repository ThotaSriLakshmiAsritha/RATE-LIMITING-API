package com.example.ratelimiting.dashboard;

import com.example.ratelimiting.policy.PolicyEntity;
import com.example.ratelimiting.policy.PolicyRepository;
import com.example.ratelimiting.policy.PolicyService;
import com.example.ratelimiting.policy.PolicySnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DashboardPolicyService {
    private final PolicyRepository policyRepository;
    private final PolicyService policyService;
    private final DashboardRealtimeService realtimeService;

    public DashboardPolicyService(
            PolicyRepository policyRepository,
            PolicyService policyService,
            DashboardRealtimeService realtimeService
    ) {
        this.policyRepository = policyRepository;
        this.policyService = policyService;
        this.realtimeService = realtimeService;
    }

    public List<DashboardPolicyDtos.PolicyResponse> getPolicies(String tenantId) {
        return policyRepository.findByTenantIdOrderByPriorityDescUpdatedAtDesc(tenantId).stream()
                .map(this::toSnapshot)
                .map(DashboardPolicyDtos.PolicyResponse::fromSnapshot)
                .toList();
    }

    public DashboardPolicyDtos.PolicyResponse createPolicy(DashboardPolicyDtos.UpdatePolicyRequest request) {
        PolicyEntity entity = new PolicyEntity();
        applyRequest(entity, request);
        PolicyEntity saved = policyService.save(entity);
        DashboardPolicyDtos.PolicyResponse response = DashboardPolicyDtos.PolicyResponse.fromSnapshot(toSnapshot(saved));
        realtimeService.publishPolicyUpdate(request.tenantId(), response);
        return response;
    }

    public DashboardPolicyDtos.PolicyResponse updatePolicy(UUID policyId, DashboardPolicyDtos.UpdatePolicyRequest request) {
        PolicyEntity entity = policyRepository.findByIdAndTenantId(policyId, request.tenantId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Policy not found"));
        applyRequest(entity, request);
        PolicyEntity saved = policyService.save(entity);
        DashboardPolicyDtos.PolicyResponse response = DashboardPolicyDtos.PolicyResponse.fromSnapshot(toSnapshot(saved));
        realtimeService.publishPolicyUpdate(request.tenantId(), response);
        return response;
    }

    private void applyRequest(PolicyEntity entity, DashboardPolicyDtos.UpdatePolicyRequest request) {
        entity.setTenantId(request.tenantId());
        entity.setScopeType(request.scopeType());
        entity.setScopeId(blankToNull(request.scopeId()));
        entity.setEndpointPattern(blankToNull(request.endpointPattern()));
        entity.setRequestsPerMinute(request.requestsPerMinute());
        entity.setBurstCapacity(request.burstCapacity());
        entity.setDimension(request.dimension());
        entity.setErrorMessage(blankToNull(request.errorMessage()));
        if (request.mode() != null) {
            entity.setMode(request.mode());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
    }

    private PolicySnapshot toSnapshot(PolicyEntity entity) {
        return new PolicySnapshot(
                entity.getId(),
                entity.getTenantId(),
                entity.getScopeType(),
                entity.getScopeId(),
                entity.getEndpointPattern(),
                entity.getRequestsPerMinute(),
                entity.getBurstCapacity(),
                entity.getDimension(),
                entity.getErrorMessage(),
                entity.getMode(),
                entity.getPriority(),
                entity.getVersion()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
