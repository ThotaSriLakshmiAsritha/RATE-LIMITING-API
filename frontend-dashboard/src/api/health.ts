import { apiRequest } from './client';

export const ping = (baseUrl: string) =>
  apiRequest<{ status: string; timestamp: string }>({ baseUrl, path: '/ping', timeoutMs: 3000 });

export const actuatorHealth = (baseUrl: string) =>
  apiRequest<{ status: string }>({ baseUrl, path: '/actuator/health', timeoutMs: 5000 });

export const fetchJwks = (baseUrl: string) =>
  apiRequest<{ keys?: unknown[] }>({ baseUrl, path: '/.well-known/jwks.json', timeoutMs: 5000 });
