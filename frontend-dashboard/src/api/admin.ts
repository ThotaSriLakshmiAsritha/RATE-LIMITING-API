import { apiRequest } from './client';
import type { AnalyticsSnapshot, Policy, PolicyFormValues } from '../types';

export const getPolicies = (baseUrl: string, token: string, tenantId = 'default') =>
  apiRequest<Policy[]>({
    baseUrl,
    path: `/api/dashboard/policies?tenantId=${encodeURIComponent(tenantId)}`,
    token,
  });

export const createPolicy = (baseUrl: string, token: string, values: PolicyFormValues) =>
  apiRequest<Policy>({
    baseUrl,
    path: '/api/dashboard/policies',
    method: 'POST',
    token,
    body: values,
  });

export const updatePolicy = (baseUrl: string, token: string, policyId: string, values: PolicyFormValues) =>
  apiRequest<Policy>({
    baseUrl,
    path: `/api/dashboard/policies/${policyId}`,
    method: 'PUT',
    token,
    body: values,
  });

export const getAnalyticsSnapshot = (baseUrl: string, token: string) =>
  apiRequest<AnalyticsSnapshot>({
    baseUrl,
    path: '/api/dashboard/analytics',
    token,
  });
