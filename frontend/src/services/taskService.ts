import { apiClient } from './apiClient';
import { Task, CreateTaskInput, PageResponse, TaskStatus, TaskPriority } from '../types';

export interface TaskQueryParams {
  status?: TaskStatus;
  priority?: TaskPriority;
  goalId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export const taskService = {
  async getTasks(params?: TaskQueryParams): Promise<PageResponse<Task>> {
    const response = await apiClient.get<PageResponse<Task>>('/api/v1/tasks', { params });
    return response.data;
  },

  async getTaskById(id: number): Promise<Task> {
    const response = await apiClient.get<Task>(`/api/v1/tasks/${id}`);
    return response.data;
  },

  async createTask(data: CreateTaskInput): Promise<Task> {
    const response = await apiClient.post<Task>('/api/v1/tasks', data);
    return response.data;
  },

  async completeTask(id: number): Promise<Task> {
    const response = await apiClient.patch<Task>(`/api/v1/tasks/${id}/complete`);
    return response.data;
  },

  async startTask(id: number): Promise<Task> {
    const response = await apiClient.patch<Task>(`/api/v1/tasks/${id}/start`);
    return response.data;
  },

  async reopenTask(id: number): Promise<Task> {
    const response = await apiClient.patch<Task>(`/api/v1/tasks/${id}/reopen`);
    return response.data;
  },

  async deleteTask(id: number): Promise<void> {
    await apiClient.delete(`/api/v1/tasks/${id}`);
  },
};
