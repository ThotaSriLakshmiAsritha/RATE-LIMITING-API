package com.example.ratelimiting.ratelimit.annotation;

import com.example.ratelimiting.config.RateLimitingProperties;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RateLimited {
    int requestsPerMinute() default -1;

    int requestsPerHour() default -1;

    int burstCapacity() default -1;

    RateLimitingProperties.Dimension dimension() default RateLimitingProperties.Dimension.IP;

    String errorMessage() default "";
}

