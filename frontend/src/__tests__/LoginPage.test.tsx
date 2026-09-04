import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '../pages/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authService } from '../services/authService';

vi.mock('../services/authService', () => ({
  authService: {
    login: vi.fn(),
    getCurrentUser: vi.fn(),
    register: vi.fn(),
  },
}));

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders login form with input controls and password toggle', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <ToastProvider>
            <LoginPage />
          </ToastProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText(/developer@devsphere.io/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/••••••••••••/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('displays client-side validation errors when submitting empty form', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <ToastProvider>
            <LoginPage />
          </ToastProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    const submitBtn = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/email is required/i)).toBeInTheDocument();
      expect(screen.getByText(/password is required/i)).toBeInTheDocument();
    });
  });

  it('handles successful login and stores token', async () => {
    vi.mocked(authService.login).mockResolvedValueOnce({
      accessToken: 'valid-jwt-token-sample',
      tokenType: 'Bearer',
      expiresIn: 3600,
    });
    vi.mocked(authService.getCurrentUser).mockResolvedValueOnce({
      id: 1,
      email: 'valid@devsphere.io',
      displayName: 'Dev User',
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ToastProvider>
            <LoginPage />
          </ToastProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    const emailInput = screen.getByPlaceholderText(/developer@devsphere.io/i);
    const passwordInput = screen.getByPlaceholderText(/••••••••••••/i);

    fireEvent.change(emailInput, { target: { value: 'test@devsphere.io' } });
    fireEvent.change(passwordInput, { target: { value: 'Password123!' } });

    const submitBtn = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authService.login).toHaveBeenCalledWith({
        email: 'test@devsphere.io',
        password: 'Password123!',
      });
      expect(localStorage.getItem('devsphere_token')).toBe('valid-jwt-token-sample');
    });
  });
});
