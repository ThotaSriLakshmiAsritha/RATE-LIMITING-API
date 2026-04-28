import { apiRequest } from './client';

export const getProducts = (baseUrl: string, token: string) =>
  apiRequest<unknown[]>({ baseUrl, path: '/api/v1/products', token });

export const createProduct = (baseUrl: string, token: string) =>
  apiRequest({
    baseUrl,
    path: '/api/v1/products',
    method: 'POST',
    token,
    body: { name: 'Demo Product', price: 9.99 },
  });

export const getAdminProducts = (baseUrl: string, token: string) =>
  apiRequest<unknown[]>({ baseUrl, path: '/api/v1/products/admin', token });
