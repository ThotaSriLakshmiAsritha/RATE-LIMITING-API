package com.example.ratelimiting.dashboard;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class DashboardRealtimeService {
    private static final long SSE_TIMEOUT_MS = 0L;

    private final DashboardAnalyticsService analyticsService;
    private final Map<String, Subscriber> emitters = new ConcurrentHashMap<>();

    public DashboardRealtimeService(DashboardAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public SseEmitter subscribe(String tenantId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(emitterId, new Subscriber(tenantId, emitter));
        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError(ex -> emitters.remove(emitterId));

        send(emitterId, emitter, "connected", Map.of("tenantId", tenantId));
        send(emitterId, emitter, "analytics", analyticsService.currentSnapshot());
        return emitter;
    }

    public void publishPolicyUpdate(String tenantId, Object payload) {
        DashboardRealtimeEvent event = new DashboardRealtimeEvent("policy_update", Instant.now(), payload);
        emitters.forEach((id, subscriber) -> {
            if (subscriber.tenantId().equals(tenantId)) {
                send(id, subscriber.emitter(), "policy_update", event);
                send(id, subscriber.emitter(), "analytics", analyticsService.currentSnapshot());
            }
        });
    }

    @Scheduled(fixedDelayString = "${dashboard.analytics.push-interval-ms:5000}")
    public void publishAnalyticsSnapshot() {
        if (emitters.isEmpty()) {
            return;
        }
        DashboardRealtimeEvent event = new DashboardRealtimeEvent("analytics", Instant.now(), analyticsService.currentSnapshot());
        emitters.forEach((id, subscriber) -> send(id, subscriber.emitter(), "analytics", event));
    }

    private void send(String emitterId, SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitterId);
            emitter.complete();
        }
    }

    private record Subscriber(String tenantId, SseEmitter emitter) {
    }
}
