import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { RegisterPage } from '../pages/RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authService } from '../services/authService';

vi.mock('../services/authService', () => ({
  authService: {
    register: vi.fn(),
    login: vi.fn(),
    getCurrentUser: vi.fn(),
  },
}));

describe('RegisterPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('validates password mismatch on registration submission', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <ToastProvider>
            <RegisterPage />
          </ToastProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    const emailInput = screen.getByPlaceholderText(/developer@devsphere.io/i);
    const passwordInputs = screen.getAllByPlaceholderText(/••••••••••••/i);

    fireEvent.change(emailInput, { target: { value: 'newuser@devsphere.io' } });
    fireEvent.change(passwordInputs[0], { target: { value: 'SecurePass123!' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'MismatchPass' } });

    const submitBtn = screen.getByRole('button', { name: /create account/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    });
  });

  it('submits registration successfully and triggers auto-login', async () => {
    vi.mocked(authService.register).mockResolvedValueOnce({
      id: 201,
      email: 'newuser@devsphere.io',
      createdAt: '2026-09-04T00:00:00Z',
    });
    vi.mocked(authService.login).mockResolvedValueOnce({
      accessToken: 'auto-login-jwt-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    });
    vi.mocked(authService.getCurrentUser).mockResolvedValueOnce({
      id: 201,
      email: 'newuser@devsphere.io',
      displayName: 'New User',
    });

    render(
      <MemoryRouter>
        <AuthProvider>
          <ToastProvider>
            <RegisterPage />
          </ToastProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    const emailInput = screen.getByPlaceholderText(/developer@devsphere.io/i);
    const passwordInputs = screen.getAllByPlaceholderText(/••••••••••••/i);

    fireEvent.change(emailInput, { target: { value: 'newuser@devsphere.io' } });
    fireEvent.change(passwordInputs[0], { target: { value: 'SecurePass123!' } });
    fireEvent.change(passwordInputs[1], { target: { value: 'SecurePass123!' } });

    const submitBtn = screen.getByRole('button', { name: /create account/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authService.register).toHaveBeenCalledWith({
        email: 'newuser@devsphere.io',
        password: 'SecurePass123!',
      });
    });
  });
});
