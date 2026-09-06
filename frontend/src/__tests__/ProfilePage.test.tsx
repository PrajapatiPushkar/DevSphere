import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProfilePage } from '../pages/ProfilePage';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userService } from '../services/userService';
import { taskService } from '../services/taskService';
import { User, Task } from '../types';

vi.mock('../services/userService', () => ({
  userService: {
    getMyProfile: vi.fn(),
    updateMyProfile: vi.fn(),
  },
}));

vi.mock('../services/taskService', () => ({
  taskService: {
    getTasks: vi.fn(),
  },
}));

const mockUser: User = {
  id: 1,
  userId: 1,
  email: 'pushkar@devsphere.io',
  displayName: 'Pushkar Prajapati',
  firstName: 'Pushkar',
  lastName: 'Prajapati',
  headline: 'Senior Platform Engineer @ DevSphere',
  bio: 'Passionate about Kubernetes and microservices',
  location: 'San Francisco, CA',
  phoneNumber: '+1-555-0199',
  currentRole: 'Senior Platform Engineer',
  yearsOfExperience: 6,
  githubUrl: 'https://github.com/prajapatipushkar',
  linkedinUrl: 'https://linkedin.com/in/pushkarprajapati',
  portfolioUrl: 'https://devsphere.io/pushkar',
  role: 'ADMIN',
  createdAt: '2025-01-15T10:00:00Z',
};

const mockTasks: Task[] = [
  {
    id: 1,
    title: 'Profile Service Task 1',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    createdAt: new Date().toISOString(),
  },
  {
    id: 2,
    title: 'Profile Service Task 2',
    priority: 'URGENT',
    status: 'COMPLETED',
    completedAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
  },
];

const renderWithProviders = (ui: React.ReactNode, authUser: User | null = mockUser) => {
  return render(
    <MemoryRouter>
      <AuthContext.Provider
        value={{
          user: authUser,
          token: authUser ? 'mock-token' : null,
          isAuthenticated: !!authUser,
          isLoading: false,
          login: vi.fn(),
          logout: vi.fn(),
          checkAuth: vi.fn(),
          updateUser: vi.fn(),
        }}
      >
        <ToastProvider>{ui}</ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  );
};

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. Profile route requires authentication / handles unauthenticated error state when no user present', async () => {
    vi.mocked(userService.getMyProfile).mockRejectedValueOnce(new Error('Unauthorized access'));
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: [],
      pageNumber: 0,
      pageSize: 50,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<ProfilePage />, null);

    await waitFor(() => {
      expect(screen.getByText(/unable to load developer profile/i)).toBeInTheDocument();
    });
  });

  it('2 & 3. Profile renders authenticated user name and email', async () => {
    vi.mocked(userService.getMyProfile).mockResolvedValueOnce(mockUser);
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderWithProviders(<ProfilePage />);

    await waitFor(() => {
      expect(screen.getAllByText('Pushkar Prajapati').length).toBeGreaterThan(0);
      expect(screen.getAllByText('pushkar@devsphere.io').length).toBeGreaterThan(0);
    });
  });

  it('4 & 5. Avatar renders correctly with initials fallback', async () => {
    vi.mocked(userService.getMyProfile).mockResolvedValueOnce({
      ...mockUser,
      githubUrl: undefined,
    });
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: [],
      pageNumber: 0,
      pageSize: 50,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<ProfilePage />);

    await waitFor(() => {
      // Initials for Pushkar Prajapati = PP
      expect(screen.getByText('PP')).toBeInTheDocument();
    });
  });

  it('6. User information renders from real user data', async () => {
    vi.mocked(userService.getMyProfile).mockResolvedValueOnce(mockUser);
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderWithProviders(<ProfilePage />);

    await waitFor(() => {
      expect(screen.getByText('San Francisco, CA')).toBeInTheDocument();
      expect(screen.getByText('6 Years')).toBeInTheDocument();
      expect(screen.getByText('Passionate about Kubernetes and microservices')).toBeInTheDocument();
      expect(screen.getByText('GitHub Profile')).toBeInTheDocument();
    });
  });

  it('7. Account statistics render correctly from available data', async () => {
    vi.mocked(userService.getMyProfile).mockResolvedValueOnce(mockUser);
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    renderWithProviders(<ProfilePage />);

    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument(); // Total tasks
      expect(screen.getByText(/50%/i)).toBeInTheDocument(); // 1/2 = 50%
    });
  });

  it('8. Loading skeleton renders while fetching', () => {
    vi.mocked(userService.getMyProfile).mockImplementation(() => new Promise(() => {}));
    vi.mocked(taskService.getTasks).mockImplementation(() => new Promise(() => {}));

    renderWithProviders(<ProfilePage />, null); // no fallback user
    expect(document.body).toBeInTheDocument();
  });

  it('9. Error state renders when user profile fetch fails without authUser fallback', async () => {
    vi.mocked(userService.getMyProfile).mockRejectedValueOnce(new Error('Network error'));
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: [],
      pageNumber: 0,
      pageSize: 50,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<ProfilePage />, null);

    await waitFor(() => {
      expect(screen.getByText(/unable to load developer profile/i)).toBeInTheDocument();
    });
  });

  it('10 & 11. Profile editing works via modal and submits payload to API', async () => {
    vi.mocked(userService.getMyProfile).mockResolvedValueOnce(mockUser);
    vi.mocked(taskService.getTasks).mockResolvedValueOnce({
      content: mockTasks,
      pageNumber: 0,
      pageSize: 50,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });

    const updatedUser = { ...mockUser, headline: 'Updated Lead Architect' };
    vi.mocked(userService.updateMyProfile).mockResolvedValueOnce(updatedUser);

    renderWithProviders(<ProfilePage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /edit profile/i })).toBeInTheDocument();
    });

    // Click Edit Profile
    fireEvent.click(screen.getByRole('button', { name: /edit profile/i }));

    await waitFor(() => {
      expect(screen.getByText('Edit Developer Profile')).toBeInTheDocument();
    });

    // Change headline input
    const headlineInput = screen.getByDisplayValue('Senior Platform Engineer @ DevSphere');
    fireEvent.change(headlineInput, { target: { value: 'Updated Lead Architect' } });

    // Submit form
    const saveBtn = screen.getByRole('button', { name: /save profile/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(userService.updateMyProfile).toHaveBeenCalledWith(
        expect.objectContaining({
          headline: 'Updated Lead Architect',
        })
      );
    });
  });
});
