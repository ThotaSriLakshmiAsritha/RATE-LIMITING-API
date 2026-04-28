import { apiRequest } from './client';

export type LoginResponse = {
  access_token: string;
  token_type: string;
  expires_in: number;
};

export const loginRequest = (baseUrl: string, username: string, password: string) =>
  apiRequest<LoginResponse>({
    baseUrl,
    path: '/api/auth/login',
    method: 'POST',
    body: { username, password },
  });
