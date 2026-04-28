package com.example.ratelimiting.ratelimit;

import com.example.ratelimiting.config.RateLimitingProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestCostResolver {
    private final RateLimitingProperties properties;

    public RequestCostResolver(RateLimitingProperties properties) {
        this.properties = properties;
    }

    public double resolveCost(HttpServletRequest request) {
        RateLimitingProperties.Cost costConfig = properties.getCost();
        String header = costConfig.getHeader();
        String raw = request.getHeader(header);
        double cost = 1.0;
        if (raw != null && !raw.isBlank()) {
            try {
                cost = Double.parseDouble(raw.trim());
            } catch (NumberFormatException ignored) {
                cost = 1.0;
            }
        }
        double min = costConfig.getMin();
        double max = costConfig.getMax();
        if (cost < min) {
            return min;
        }
        if (cost > max) {
            return max;
        }
        return cost;
    }
}
