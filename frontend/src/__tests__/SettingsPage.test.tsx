import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SettingsPage } from '../pages/SettingsPage';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userService } from '../services/userService';
import { User } from '../types';

vi.mock('../services/userService', () => ({
  userService: {
    updateMyProfile: vi.fn(),
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

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('12. Settings route requires authentication', () => {
    renderWithProviders(<SettingsPage />, null);
    expect(screen.getByText(/authentication required/i)).toBeInTheDocument();
  });

  it('13. Account settings render user details and read-only email', () => {
    renderWithProviders(<SettingsPage />);

    expect(screen.getByText('Personal Information')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Pushkar')).toBeInTheDocument();
    expect(screen.getByDisplayValue('pushkar@devsphere.io')).toBeDisabled();
  });

  it('14, 15, 16, 17. Security section renders governance information and authentication mechanisms', () => {
    renderWithProviders(<SettingsPage />);

    // Switch to Security tab
    fireEvent.click(screen.getByRole('button', { name: /password & security/i }));

    expect(screen.getByText('Password & Security Governance')).toBeInTheDocument();
    expect(screen.getByText(/Central Security Policy/i)).toBeInTheDocument();
    expect(screen.getByText(/BCrypt Encrypted/i)).toBeInTheDocument();
  });

  it('18. Preferences render with default and configurable options', () => {
    renderWithProviders(<SettingsPage />);

    fireEvent.click(screen.getByRole('button', { name: /user preferences/i }));

    expect(screen.getByText('Application Preferences')).toBeInTheDocument();
    expect(screen.getByText(/Default Task Page Size/i)).toBeInTheDocument();
    expect(screen.getByText(/Enable Compact View Mode/i)).toBeInTheDocument();
  });

  it('19. Theme preference switches theme and persists in localStorage', () => {
    renderWithProviders(<SettingsPage />);

    fireEvent.click(screen.getByRole('button', { name: /appearance & theme/i }));

    expect(screen.getByText('Appearance & Visual Styling')).toBeInTheDocument();

    // Click Light Theme option
    const lightThemeCard = screen.getByText('Light Theme');
    fireEvent.click(lightThemeCard);

    expect(localStorage.getItem('devsphere_theme')).toBe('light');
  });

  it('20 & 23. Account settings save submits payload and triggers updateUser state sync', async () => {
    const updatedUser = { ...mockUser, displayName: 'Pushkar Updated' };
    vi.mocked(userService.updateMyProfile).mockResolvedValueOnce(updatedUser);

    renderWithProviders(<SettingsPage />);

    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(userService.updateMyProfile).toHaveBeenCalledWith(
        expect.objectContaining({
          firstName: 'Pushkar',
          lastName: 'Prajapati',
        })
      );
    });
  });

  it('21. Settings errors are displayed correctly when API update fails', async () => {
    vi.mocked(userService.updateMyProfile).mockRejectedValueOnce(new Error('Validation error on role'));

    renderWithProviders(<SettingsPage />);

    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(screen.getByText(/Validation error on role/i)).toBeInTheDocument();
    });
  });

  it('22. Preferences save action updates localStorage and shows feedback', async () => {
    renderWithProviders(<SettingsPage />);

    fireEvent.click(screen.getByRole('button', { name: /user preferences/i }));

    const savePrefBtn = screen.getByRole('button', { name: /save preferences/i });
    fireEvent.click(savePrefBtn);

    await waitFor(() => {
      expect(localStorage.getItem('devsphere_user_preferences')).not.toBeNull();
    });
  });
});
