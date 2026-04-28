package com.example.ratelimiting.ratelimit;

public class RateLimitBackendUnavailableException extends RuntimeException {
    public RateLimitBackendUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

