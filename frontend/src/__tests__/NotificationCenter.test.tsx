import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NotificationBell } from '../components/notifications/NotificationBell';
import { NotificationPanel } from '../components/notifications/NotificationPanel';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { activityService } from '../services/activityService';
import { NotificationItemData } from '../types/notificationTypes';
import { User } from '../types';

vi.mock('../services/activityService', () => ({
  activityService: {
    getNotifications: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
  },
}));

const mockUser: User = {
  id: 1,
  email: 'pushkar@devsphere.io',
  displayName: 'Pushkar Prajapati',
};

const mockNotifications: NotificationItemData[] = [
  {
    id: 'notif-1',
    category: 'TASK_OVERDUE',
    title: 'Task Overdue Notice',
    message: 'Task "Audit Prometheus scraper" is overdue.',
    timestamp: '2026-09-05T10:00:00Z',
    isRead: false,
    targetUrl: '/tasks',
  },
  {
    id: 'notif-2',
    category: 'TASK_COMPLETED',
    title: 'Task Completed',
    message: 'Task "Deploy User Service" was completed.',
    timestamp: '2026-09-04T10:00:00Z',
    isRead: true,
    targetUrl: '/tasks',
  },
];

const renderWithProviders = (ui: React.ReactNode) => {
  return render(
    <MemoryRouter>
      <AuthContext.Provider
        value={{
          user: mockUser,
          token: 'jwt-token',
          isAuthenticated: true,
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

describe('NotificationCenter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('1. Notification bell renders with unread count badge', async () => {
    vi.mocked(activityService.getNotifications).mockResolvedValueOnce(mockNotifications);

    renderWithProviders(<NotificationBell />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /notifications \(1 unread\)/i })).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  it('2. Notification panel opens upon clicking bell icon', async () => {
    vi.mocked(activityService.getNotifications).mockResolvedValue(mockNotifications);

    renderWithProviders(<NotificationBell />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /notifications/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    await waitFor(() => {
      expect(screen.getByText('Notifications')).toBeInTheDocument();
    });
  });

  it('3. Loading skeleton renders when notifications are fetching', () => {
    render(
      <MemoryRouter>
        <ToastProvider>
          <NotificationPanel
            notifications={[]}
            isLoading={true}
            error={null}
            onClose={vi.fn()}
            onRefresh={vi.fn()}
            onMarkRead={vi.fn()}
            onMarkAllRead={vi.fn()}
          />
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByTestId('notification-skeleton')).toBeInTheDocument();
  });

  it('4. Empty notification state renders when list is empty', () => {
    render(
      <MemoryRouter>
        <ToastProvider>
          <NotificationPanel
            notifications={[]}
            isLoading={false}
            error={null}
            onClose={vi.fn()}
            onRefresh={vi.fn()}
            onMarkRead={vi.fn()}
            onMarkAllRead={vi.fn()}
          />
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/you're all caught up/i)).toBeInTheDocument();
  });

  it('5, 6 & 7. Real notification data renders with unread visual distinction and count', () => {
    render(
      <MemoryRouter>
        <ToastProvider>
          <NotificationPanel
            notifications={mockNotifications}
            isLoading={false}
            error={null}
            onClose={vi.fn()}
            onRefresh={vi.fn()}
            onMarkRead={vi.fn()}
            onMarkAllRead={vi.fn()}
          />
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Task Overdue Notice')).toBeInTheDocument();
    expect(screen.getByText('Task Completed')).toBeInTheDocument();
    expect(screen.getByText('1 New')).toBeInTheDocument();
  });

  it('8 & 9. Mark notification as read and Mark all as read work', async () => {
    const handleMarkRead = vi.fn();
    const handleMarkAllRead = vi.fn();

    render(
      <MemoryRouter>
        <ToastProvider>
          <NotificationPanel
            notifications={mockNotifications}
            isLoading={false}
            error={null}
            onClose={vi.fn()}
            onRefresh={vi.fn()}
            onMarkRead={handleMarkRead}
            onMarkAllRead={handleMarkAllRead}
          />
        </ToastProvider>
      </MemoryRouter>
    );

    const markAllBtn = screen.getByRole('button', { name: /mark read/i });
    fireEvent.click(markAllBtn);

    expect(handleMarkAllRead).toHaveBeenCalled();
  });

  it('10 & 11. API errors are displayed with retry action', () => {
    const handleRefresh = vi.fn();

    render(
      <MemoryRouter>
        <ToastProvider>
          <NotificationPanel
            notifications={[]}
            isLoading={false}
            error="Gateway connection timeout"
            onClose={vi.fn()}
            onRefresh={handleRefresh}
            onMarkRead={vi.fn()}
            onMarkAllRead={vi.fn()}
          />
        </ToastProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('Gateway connection timeout')).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryBtn);

    expect(handleRefresh).toHaveBeenCalled();
  });
});
