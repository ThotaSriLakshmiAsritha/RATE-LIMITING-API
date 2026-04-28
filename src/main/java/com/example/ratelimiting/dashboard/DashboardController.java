package com.example.ratelimiting.dashboard;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardPolicyService dashboardPolicyService;
    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final DashboardRealtimeService dashboardRealtimeService;

    public DashboardController(
            DashboardPolicyService dashboardPolicyService,
            DashboardAnalyticsService dashboardAnalyticsService,
            DashboardRealtimeService dashboardRealtimeService
    ) {
        this.dashboardPolicyService = dashboardPolicyService;
        this.dashboardAnalyticsService = dashboardAnalyticsService;
        this.dashboardRealtimeService = dashboardRealtimeService;
    }

    @GetMapping("/policies")
    public List<DashboardPolicyDtos.PolicyResponse> getPolicies(
            @RequestParam(defaultValue = "default") String tenantId
    ) {
        return dashboardPolicyService.getPolicies(tenantId);
    }

    @PostMapping("/policies")
    public DashboardPolicyDtos.PolicyResponse createPolicy(
            @Valid @RequestBody DashboardPolicyDtos.UpdatePolicyRequest request
    ) {
        return dashboardPolicyService.createPolicy(request);
    }

    @PutMapping("/policies/{policyId}")
    public DashboardPolicyDtos.PolicyResponse updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody DashboardPolicyDtos.UpdatePolicyRequest request
    ) {
        return dashboardPolicyService.updatePolicy(policyId, request);
    }

    @GetMapping("/analytics")
    public DashboardAnalyticsDtos.AnalyticsResponse analytics() {
        return dashboardAnalyticsService.currentSnapshot();
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(defaultValue = "default") String tenantId) {
        return dashboardRealtimeService.subscribe(tenantId);
    }
}
