import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TasksPage } from '../pages/TasksPage';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { taskService } from '../services/taskService';
import { Task, User } from '../types';

vi.mock('../services/taskService', () => ({
  taskService: {
    getTasks: vi.fn(),
    createTask: vi.fn(),
    updateTask: vi.fn(),
    completeTask: vi.fn(),
    startTask: vi.fn(),
    reopenTask: vi.fn(),
    cancelTask: vi.fn(),
    deleteTask: vi.fn(),
  },
}));

const mockUser: User = {
  id: 1,
  email: 'pushkar@devsphere.io',
  displayName: 'Pushkar Prajapati',
};

const mockTasks: Task[] = [
  {
    id: 101,
    title: 'Audit Prometheus metrics scrape config',
    description: 'Verify custom Micrometer counters for Auth service',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    dueDate: '2026-09-10T00:00:00Z',
    createdAt: '2026-09-01T10:00:00Z',
  },
  {
    id: 102,
    title: 'Configure Redis cache eviction policy',
    description: 'Evict user profiles on update event',
    priority: 'URGENT',
    status: 'TODO',
    dueDate: '2026-09-02T00:00:00Z',
    overdue: true,
    createdAt: '2026-09-02T10:00:00Z',
  },
  {
    id: 103,
    title: 'Deploy User Service to K8s cluster',
    description: 'Apply deployment and ingress configs',
    priority: 'LOW',
    status: 'COMPLETED',
    completedAt: '2026-09-03T10:00:00Z',
    createdAt: '2026-08-30T10:00:00Z',
  },
];

const renderWithProviders = (ui: React.ReactNode) => {
  return render(
    <MemoryRouter>
      <AuthContext.Provider
        value={{
          user: mockUser,
          token: 'mock-jwt-token',
          isAuthenticated: true,
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

describe('TasksPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(taskService.getTasks).mockResolvedValue({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 20,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  it('renders loading skeleton initially before task data is loaded', () => {
    vi.mocked(taskService.getTasks).mockImplementation(
      () => new Promise(() => {}) // pending promise
    );

    renderWithProviders(<TasksPage />);
    expect(screen.getByTestId('task-list-skeleton')).toBeInTheDocument();
  });

  it('renders task list with titles, priority badges, and status badges', async () => {
    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
      expect(screen.getAllByText('Configure Redis cache eviction policy')[0]).toBeInTheDocument();
      expect(screen.getAllByText('Deploy User Service to K8s cluster')[0]).toBeInTheDocument();
    });

    expect(screen.getAllByText('URGENT')[0]).toBeInTheDocument();
    expect(screen.getAllByText('IN PROGRESS')[0]).toBeInTheDocument();
    expect(screen.getAllByText('COMPLETED')[0]).toBeInTheDocument();
  });

  it('filters task list by search query input', async () => {
    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/search tasks by title or description/i);
    fireEvent.change(searchInput, { target: { value: 'Prometheus' } });

    expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    expect(screen.queryByText('Configure Redis cache eviction policy')).not.toBeInTheDocument();
  });

  it('filters task list by status dropdown selection', async () => {
    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(taskService.getTasks).toHaveBeenCalled();
    });

    const statusSelect = screen.getByLabelText(/status/i);
    fireEvent.change(statusSelect, { target: { value: 'IN_PROGRESS' } });

    await waitFor(() => {
      expect(taskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'IN_PROGRESS' })
      );
    });
  });

  it('opens create task modal and submits new task form', async () => {
    vi.mocked(taskService.createTask).mockResolvedValueOnce({
      id: 104,
      title: 'New Unit Test Task',
      priority: 'HIGH',
      status: 'TODO',
      createdAt: new Date().toISOString(),
    });

    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });

    const newTaskBtn = screen.getAllByRole('button', { name: /new task/i })[0];
    fireEvent.click(newTaskBtn);

    const titleInput = screen.getByPlaceholderText(/implement kafka event consumer/i);
    fireEvent.change(titleInput, { target: { value: 'New Unit Test Task' } });

    const submitBtn = screen.getByRole('button', { name: /^create task$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(taskService.createTask).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'New Unit Test Task',
        })
      );
    });
  });

  it('opens edit task modal and submits updated task data', async () => {
    vi.mocked(taskService.updateTask).mockResolvedValueOnce({
      ...mockTasks[0],
      title: 'Updated Prometheus metrics scrape config',
    });

    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });

    // Open task action dropdown for first row
    const actionBtns = screen.getAllByTitle('Task actions');
    fireEvent.click(actionBtns[0]);

    const editMenuItem = screen.getAllByText('Edit Task')[0];
    fireEvent.click(editMenuItem);

    // Verify modal pre-populates existing title
    const titleInput = screen.getByDisplayValue('Audit Prometheus metrics scrape config');
    fireEvent.change(titleInput, { target: { value: 'Updated Prometheus metrics scrape config' } });

    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(taskService.updateTask).toHaveBeenCalledWith(
        101,
        expect.objectContaining({
          title: 'Updated Prometheus metrics scrape config',
        })
      );
    });
  });

  it('triggers complete task action from dropdown menu', async () => {
    vi.mocked(taskService.completeTask).mockResolvedValueOnce({
      ...mockTasks[0],
      status: 'COMPLETED',
    });

    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });

    const actionBtns = screen.getAllByTitle('Task actions');
    fireEvent.click(actionBtns[0]);

    const completeMenuItem = screen.getAllByText('Mark Completed')[0];
    fireEvent.click(completeMenuItem);

    await waitFor(() => {
      expect(taskService.completeTask).toHaveBeenCalledWith(101);
    });
  });

  it('triggers task deletion confirmation modal and calls delete API', async () => {
    vi.mocked(taskService.deleteTask).mockResolvedValueOnce();

    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });

    const actionBtns = screen.getAllByTitle('Task actions');
    fireEvent.click(actionBtns[0]);

    const deleteMenuItem = screen.getAllByText('Delete Task')[0];
    fireEvent.click(deleteMenuItem);

    // Confirmation modal should open
    expect(screen.getByText(/are you sure you want to delete this task\?/i)).toBeInTheDocument();

    const confirmDeleteBtn = screen.getByRole('button', { name: 'Delete Task' });
    fireEvent.click(confirmDeleteBtn);

    await waitFor(() => {
      expect(taskService.deleteTask).toHaveBeenCalledWith(101);
    });
  });

  it('renders empty state when no tasks match current search criteria', async () => {
    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/search tasks by title or description/i);
    fireEvent.change(searchInput, { target: { value: 'NonExistentTaskQueryXYZ' } });

    expect(screen.getByText('No matching tasks found')).toBeInTheDocument();
  });

  it('renders error state and retries on failure', async () => {
    vi.mocked(taskService.getTasks).mockRejectedValueOnce(new Error('Network Error'));

    renderWithProviders(<TasksPage />);

    await waitFor(() => {
      expect(screen.getByText('Unable to load tasks')).toBeInTheDocument();
      expect(screen.getByText('Network Error')).toBeInTheDocument();
    });

    vi.mocked(taskService.getTasks).mockResolvedValue({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 20,
      totalElements: 3,
      totalPages: 1,
      first: true,
      last: true,
    });

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getAllByText('Audit Prometheus metrics scrape config')[0]).toBeInTheDocument();
    });
  });
});

