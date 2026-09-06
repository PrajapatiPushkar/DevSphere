import { apiClient } from './apiClient';
import { User, UpdateUserProfileInput } from '../types';

export const userService = {
  async getMyProfile(): Promise<User> {
    const response = await apiClient.get<User>('/api/v1/users/me');
    return response.data;
  },

  async updateMyProfile(input: UpdateUserProfileInput): Promise<User> {
    const response = await apiClient.put<User>('/api/v1/users/me', input);
    return response.data;
  },
};
