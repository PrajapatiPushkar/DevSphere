import React from 'react';
import { render, screen, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { authService } from '../services/authService';

vi.mock('../services/authService', () => ({
  authService: {
    getCurrentUser: vi.fn(),
    login: vi.fn(),
    register: vi.fn(),
  },
}));

const TestComponent = () => {
  const { user, isAuthenticated, isLoading, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="loading">{isLoading ? 'Loading' : 'Ready'}</span>
      <span data-testid="auth">{isAuthenticated ? 'Authenticated' : 'Unauthenticated'}</span>
      <span data-testid="email">{user?.email || 'No Email'}</span>
      <button onClick={() => login('token-123', { id: 1, email: 'test@devsphere.io' })}>Login</button>
      <button onClick={logout}>Logout</button>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('provides unauthenticated state when no token exists in localStorage', async () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth').textContent).toBe('Unauthenticated');
    expect(screen.getByTestId('email').textContent).toBe('No Email');
  });

  it('restores authentication when token exists in localStorage', async () => {
    localStorage.setItem('devsphere_token', 'valid-token');
    vi.mocked(authService.getCurrentUser).mockResolvedValueOnce({
      id: 1,
      email: 'restored@devsphere.io',
      displayName: 'Restored User',
    });

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await act(async () => {
      await Promise.resolve();
    });

    expect(screen.getByTestId('auth').textContent).toBe('Authenticated');
    expect(screen.getByTestId('email').textContent).toBe('restored@devsphere.io');
  });

  it('clears token on invalid auth restoration', async () => {
    localStorage.setItem('devsphere_token', 'invalid-token');
    vi.mocked(authService.getCurrentUser).mockRejectedValueOnce(new Error('Unauthorized'));

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await act(async () => {
      await Promise.resolve();
    });

    expect(localStorage.getItem('devsphere_token')).toBeNull();
    expect(screen.getByTestId('auth').textContent).toBe('Unauthenticated');
  });
});
