import { extractRateLimitHeaders } from '../utils/headers';
import type { RateLimitHeaders } from '../types';

export class ApiError extends Error {
  status?: number;
  data?: unknown;
  headers?: Headers;
  isNetworkError?: boolean;
  rateLimitHeaders?: RateLimitHeaders;

  constructor(message: string, options: Partial<ApiError> = {}) {
    super(message);
    Object.assign(this, options);
  }
}

type RequestOptions = {
  path: string;
  method?: string;
  body?: unknown;
  token?: string | null;
  baseUrl: string;
  timeoutMs?: number;
  headers?: Record<string, string>;
};

export async function apiRequest<T>({
  path,
  method = 'GET',
  body,
  token,
  baseUrl,
  timeoutMs = 10000,
  headers = {},
}: RequestOptions): Promise<{ data: T; status: number; headers: Headers }> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${baseUrl}${path}`, {
      method,
      signal: controller.signal,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    const contentType = response.headers.get('content-type') ?? '';
    const payload = contentType.includes('application/json') ? await response.json() : await response.text();

    if (!response.ok) {
      throw new ApiError(`HTTP ${response.status}`, {
        status: response.status,
        data: payload,
        headers: response.headers,
        rateLimitHeaders: extractRateLimitHeaders(response.headers),
      });
    }

    return { data: payload as T, status: response.status, headers: response.headers };
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError('Network error', { isNetworkError: true });
  } finally {
    window.clearTimeout(timer);
  }
}
