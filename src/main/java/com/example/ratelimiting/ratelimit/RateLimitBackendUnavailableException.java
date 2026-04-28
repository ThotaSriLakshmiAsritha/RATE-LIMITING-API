package com.example.ratelimiting.ratelimit;

public class RateLimitBackendUnavailableException extends RuntimeException {
    private final BackendType backendType;
    private final boolean retryable;

    public RateLimitBackendUnavailableException(String message, Throwable cause) {
        this(message, BackendType.REDIS, true, cause);
    }

    public RateLimitBackendUnavailableException(
            String message,
            BackendType backendType,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.backendType = backendType;
        this.retryable = retryable;
    }

    public BackendType backendType() {
        return backendType;
    }

    public boolean retryable() {
        return retryable;
    }
}

