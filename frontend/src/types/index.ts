export interface User {
  id?: number | string;
  userId?: number | string;
  email?: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  headline?: string;
  bio?: string;
  location?: string;
  currentRole?: string;
  yearsOfExperience?: number;
  githubUrl?: string;
  linkedinUrl?: string;
  role?: string;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export interface ApiError {
  code?: string;
  message: string;
  status?: number;
  timestamp?: string;
}

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  title?: string;
  message: string;
  type: ToastType;
  duration?: number;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'ARCHIVED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Task {
  id: number;
  goalId?: number | null;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string | null;
  completedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
  overdue?: boolean;
}

export interface CreateTaskInput {
  title: string;
  description?: string;
  priority: TaskPriority;
  dueDate?: string;
  goalId?: number;
}

export interface UpdateTaskInput {
  title: string;
  description?: string;
  priority: TaskPriority;
  dueDate?: string;
  goalId?: number;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface DashboardStats {
  total: number;
  pending: number;
  completed: number;
  overdue: number;
  completionPercentage: number;
}

export interface TaskSummary {
  id: number;
  title: string;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate?: string;
}

export interface SystemMetricSummary {
  label: string;
  value: string | number;
  change: string;
  trend: 'up' | 'down' | 'neutral';
}

