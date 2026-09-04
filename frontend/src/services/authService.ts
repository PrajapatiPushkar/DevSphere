import { apiClient } from './apiClient';
import { User } from '../types';

export interface LoginCredentials {
  email: string;
  password?: string;
}

export interface RegisterCredentials {
  email: string;
  password?: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterResponse {
  id: number;
  email: string;
  createdAt: string;
}

export const authService = {
  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', credentials);
    return response.data;
  },

  async register(credentials: RegisterCredentials): Promise<RegisterResponse> {
    const response = await apiClient.post<RegisterResponse>('/api/v1/auth/register', credentials);
    return response.data;
  },

  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/api/v1/users/me');
    return response.data;
  },
};
