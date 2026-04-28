export type ToastTone = 'success' | 'warning' | 'error' | 'info';

export type RateLimitHeaders = {
  limit: number | null;
  remaining: number | null;
  reset: number | null;
  retryAfter: number | null;
};

export type RequestEntry = {
  id: string;
  timestamp: string;
  endpoint: string;
  method: string;
  statusCode: number;
  latencyMs: number;
  rateLimitHeaders: RateLimitHeaders;
  responseBody: unknown;
};

export type Metrics = {
  total: number;
  successful: number;
  throttled: number;
  errors: number;
};

export type ToastItem = {
  id: string;
  title: string;
  description: string;
  tone: ToastTone;
  countdownSeconds?: number | null;
};

export type AuthState = {
  authToken: string | null;
  username: string | null;
  role: string | null;
  tokenExpiry: number | null;
};

export type HealthState = {
  status: 'checking' | 'online' | 'offline';
  latencyMs: number | null;
  lastCheckedAt: string | null;
};

export type AppState = AuthState & {
  apiBaseUrl: string;
  requestHistory: RequestEntry[];
  latestHeaders: RateLimitHeaders | null;
  backendHealth: HealthState;
  toasts: ToastItem[];
  sidebarCollapsed: boolean;
};

export type Policy = {
  id: string;
  tenantId: string;
  scopeType: string;
  scopeId: string | null;
  endpointPattern: string | null;
  requestsPerMinute: number | null;
  burstCapacity: number | null;
  dimension: string | null;
  errorMessage: string | null;
  mode: string | null;
  priority: number | null;
  version: number;
};

export type PolicyFormValues = {
  tenantId: string;
  scopeType: string;
  scopeId: string;
  endpointPattern: string;
  requestsPerMinute: number;
  burstCapacity: number;
  dimension: string;
  errorMessage: string;
  mode: string;
  priority: number;
  enabled: boolean;
};

export type AnalyticsSnapshot = {
  generatedAt: string;
  rateLimitStats: {
    allowedRequests: number;
    deniedRequests: number;
    unavailableRequests: number;
    redisFailures: number;
    averageDecisionLatencyMs: number;
    averageRedisTimeMs: number;
  };
  usageMetrics: {
    requestObservations: number;
    averageRequestLatencyMs: number;
    topDecisions: Array<{ tagValue: string; count: number }>;
    backendDecisions: Record<string, number>;
  };
};

export type StreamEvent = {
  id: string;
  type: string;
  emittedAt: string;
  payload: unknown;
};
