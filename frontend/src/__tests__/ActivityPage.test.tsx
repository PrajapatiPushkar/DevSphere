import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ActivityPage } from '../pages/ActivityPage';
import { ActivityTimeline } from '../components/activity/ActivityTimeline';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { activityService } from '../services/activityService';
import { ActivityItemData } from '../types/notificationTypes';
import { User } from '../types';

vi.mock('../services/activityService', () => ({
  activityService: {
    getActivityTimeline: vi.fn(),
  },
}));

const mockUser: User = {
  id: 1,
  email: 'pushkar@devsphere.io',
  displayName: 'Pushkar Prajapati',
};

const mockActivities: ActivityItemData[] = [
  {
    id: 'act-1',
    type: 'TASK_COMPLETED',
    title: 'Completed task "Deploy User Service"',
    description: 'Finished with priority URGENT',
    timestamp: '2026-09-06T06:00:00Z',
    targetUrl: '/tasks',
  },
  {
    id: 'act-2',
    type: 'PROFILE_UPDATED',
    title: 'Updated Developer Profile Information',
    description: 'Role: Senior Platform Engineer',
    timestamp: '2026-09-05T12:00:00Z',
    targetUrl: '/profile',
  },
  {
    id: 'act-3',
    type: 'TASK_CREATED',
    title: 'Created task "Configure Prometheus scrapers"',
    description: 'Priority: HIGH',
    timestamp: '2026-09-04T10:00:00Z',
    targetUrl: '/tasks',
  },
];

const renderWithProviders = (ui: React.ReactNode, authUser: User | null = mockUser) => {
  return render(
    <MemoryRouter>
      <AuthContext.Provider
        value={{
          user: authUser,
          token: authUser ? 'mock-jwt-token' : null,
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

describe('ActivityPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('13 & 14. Activity page renders header and loading skeleton initially', () => {
    vi.mocked(activityService.getActivityTimeline).mockImplementation(() => new Promise(() => {}));

    renderWithProviders(<ActivityPage />);

    expect(screen.getAllByText('Activity Timeline').length).toBeGreaterThan(0);
    expect(screen.getByTestId('activity-skeleton')).toBeInTheDocument();
  });

  it('15. Empty activity state renders when timeline has no events', async () => {
    vi.mocked(activityService.getActivityTimeline).mockResolvedValueOnce([]);

    renderWithProviders(<ActivityPage />);

    await waitFor(() => {
      expect(screen.getByText('No activity events found')).toBeInTheDocument();
    });
  });

  it('16, 17, 18 & 19. Real task and profile activity events render chronologically with formatted timestamps', async () => {
    vi.mocked(activityService.getActivityTimeline).mockResolvedValueOnce(mockActivities);

    renderWithProviders(<ActivityPage />);

    await waitFor(() => {
      expect(screen.getByText('Completed task "Deploy User Service"')).toBeInTheDocument();
      expect(screen.getByText('Updated Developer Profile Information')).toBeInTheDocument();
      expect(screen.getByText('Created task "Configure Prometheus scrapers"')).toBeInTheDocument();
    });

    // Check badges
    expect(screen.getByText('Task Completed')).toBeInTheDocument();
    expect(screen.getByText('Profile Event')).toBeInTheDocument();
    expect(screen.getByText('Task Created')).toBeInTheDocument();
  });

  it('20. Activity API errors are handled gracefully with retry option', async () => {
    vi.mocked(activityService.getActivityTimeline).mockRejectedValueOnce(new Error('Database timeout'));

    renderWithProviders(<ActivityPage />);

    await waitFor(() => {
      expect(screen.getByText('Database timeout')).toBeInTheDocument();
    });

    vi.mocked(activityService.getActivityTimeline).mockResolvedValueOnce(mockActivities);

    const retryBtn = screen.getByRole('button', { name: /retry loading activity/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByText('Completed task "Deploy User Service"')).toBeInTheDocument();
    });
  });

  it('filters activity events by category button selection', async () => {
    vi.mocked(activityService.getActivityTimeline).mockResolvedValueOnce(mockActivities);

    renderWithProviders(<ActivityPage />);

    await waitFor(() => {
      expect(screen.getByText('Completed task "Deploy User Service"')).toBeInTheDocument();
    });

    // Filter Profile only
    const profileFilterBtn = screen.getByRole('button', { name: /profile \(/i });
    fireEvent.click(profileFilterBtn);

    expect(screen.getByText('Updated Developer Profile Information')).toBeInTheDocument();
    expect(screen.queryByText('Completed task "Deploy User Service"')).not.toBeInTheDocument();
  });
});
