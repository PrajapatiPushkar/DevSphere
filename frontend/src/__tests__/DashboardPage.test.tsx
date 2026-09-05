import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from '../pages/DashboardPage';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { taskService } from '../services/taskService';
import { Task, User } from '../types';

vi.mock('../services/taskService', () => ({
  taskService: {
    getTasks: vi.fn(),
    createTask: vi.fn(),
    completeTask: vi.fn(),
  },
}));

const mockUser: User = {
  id: 1,
  email: 'pushkar@devsphere.io',
  displayName: 'Pushkar Prajapati',
  firstName: 'Pushkar',
  lastName: 'Prajapati',
  currentRole: 'Senior Platform Engineer',
};

const mockTasks: Task[] = [
  {
    id: 1,
    title: 'Configure Prometheus scrapers',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    dueDate: new Date(Date.now() + 86400000).toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 2,
    title: 'Deploy User Service Pods',
    priority: 'URGENT',
    status: 'COMPLETED',
    completedAt: new Date().toISOString(),
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 3,
    title: 'Fix Kafka outbox delay',
    priority: 'MEDIUM',
    status: 'TODO',
    dueDate: new Date(Date.now() - 86400000).toISOString(), // Overdue
    overdue: true,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

const renderWithProviders = (ui: React.ReactNode, authUser: User | null = mockUser) => {
  return render(
    <MemoryRouter>
      <AuthContext.Provider
        value={{
          user: authUser,
          token: 'mock-jwt-token',
          isAuthenticated: !!authUser,
          isLoading: false,
          login: vi.fn(),
          logout: vi.fn(),
          checkAuth: vi.fn(),
        }}
      >
        <ToastProvider>{ui}</ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  );
};

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading skeleton initially before data arrives', () => {
    vi.mocked(taskService.getTasks).mockImplementation(
      () => new Promise(() => {}) // pending promise
    );

    renderWithProviders(<DashboardPage />);
    expect(screen.getByTestId('dashboard-skeleton')).toBeInTheDocument();
  });

  it('renders welcome banner with authenticated user name and calculated statistics', async () => {
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderWithProviders(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/welcome back, pushkar prajapati/i)).toBeInTheDocument();
    });

    // Check stats: 3 total, 2 pending (IN_PROGRESS & TODO), 1 completed, 1 overdue
    expect(screen.getByText('3')).toBeInTheDocument(); // Total tasks
    expect(screen.getByText('2')).toBeInTheDocument(); // Pending tasks
    expect(screen.getAllByText('1').length).toBeGreaterThanOrEqual(2); // Completed & Overdue tasks
    expect(screen.getByText('33%')).toBeInTheDocument(); // Completion (1/3 = 33%)
  });

  it('renders upcoming tasks prioritized by status and due date', async () => {
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderWithProviders(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Configure Prometheus scrapers')).toBeInTheDocument();
      expect(screen.getByText('Fix Kafka outbox delay')).toBeInTheDocument();
    });
  });

  it('handles empty task list gracefully with empty state and 0% completion without NaN', async () => {
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: [],
      pageNumber: 0,
      pageSize: 50,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/no tasks yet/i)).toBeInTheDocument();
      expect(screen.getByText(/create your first task to get started/i)).toBeInTheDocument();
    });

    // 0 tasks -> percentage is 0%, no NaN
    expect(screen.queryByText(/NaN/i)).not.toBeInTheDocument();
  });

  it('renders error state when API fails and retries upon clicking Try Again', async () => {
    vi.mocked(taskService.getTasks).mockRejectedValueOnce(new Error('Gateway timeout'));

    renderWithProviders(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/unable to load dashboard data/i)).toBeInTheDocument();
      expect(screen.getByText(/gateway timeout/i)).toBeInTheDocument();
    });

    // Mock successful retry
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText(/welcome back, pushkar prajapati/i)).toBeInTheDocument();
    });
  });

  it('opens create task modal and submits new task to API service', async () => {
    vi.mocked(taskService.getTasks).mockResolvedValue({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    vi.mocked(taskService.createTask).mockResolvedValueOnce({
      id: 4,
      title: 'New Integration Test Task',
      priority: 'HIGH',
      status: 'TODO',
      createdAt: new Date().toISOString(),
    });

    renderWithProviders(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/welcome back, pushkar prajapati/i)).toBeInTheDocument();
    });

    // Click New Task button
    const newTaskBtns = screen.getAllByRole('button', { name: /new task/i });
    fireEvent.click(newTaskBtns[0]);

    // Fill form inside modal
    const titleInput = screen.getByPlaceholderText(/implement redis eviction policy/i);
    fireEvent.change(titleInput, { target: { value: 'New Integration Test Task' } });

    // Submit form
    const submitBtn = screen.getByRole('button', { name: /^create task$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(taskService.createTask).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'New Integration Test Task',
          priority: 'MEDIUM',
        })
      );
    });
  });
});
