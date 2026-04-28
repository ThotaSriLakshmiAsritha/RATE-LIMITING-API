import type { RateLimitHeaders } from '../types';

const parseNumber = (value: string | null) => {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export function extractRateLimitHeaders(headers: Headers): RateLimitHeaders {
  return {
    limit: parseNumber(headers.get('x-ratelimit-limit')),
    remaining: parseNumber(headers.get('x-ratelimit-remaining')),
    reset: parseNumber(headers.get('x-ratelimit-reset')),
    retryAfter: parseNumber(headers.get('retry-after')),
  };
}
