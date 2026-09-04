export interface User {
  id: number | string;
  email: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
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

export interface TaskSummary {
  id: number;
  title: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  dueDate?: string;
}

export interface SystemMetricSummary {
  label: string;
  value: string | number;
  change: string;
  trend: 'up' | 'down' | 'neutral';
}
