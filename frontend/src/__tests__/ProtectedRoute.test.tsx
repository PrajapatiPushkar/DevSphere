import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '../components/layout/ProtectedRoute';
import { AuthContext } from '../context/AuthContext';
import { describe, it, expect } from 'vitest';

describe('ProtectedRoute', () => {
  it('renders children when user is authenticated', () => {
    const mockAuthValue = {
      user: { id: 1, email: 'user@devsphere.io' },
      token: 'jwt-token',
      isAuthenticated: true,
      isLoading: false,
      login: () => {},
      logout: () => {},
      checkAuth: async () => {},
    };

    render(
      <AuthContext.Provider value={mockAuthValue}>
        <MemoryRouter initialEntries={['/protected']}>
          <Routes>
            <Route
              path="/protected"
              element={
                <ProtectedRoute>
                  <div data-testid="protected-content">Protected Content</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.getByTestId('protected-content')).toBeInTheDocument();
  });

  it('redirects to /login when user is unauthenticated', () => {
    const mockAuthValue = {
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
      login: () => {},
      logout: () => {},
      checkAuth: async () => {},
    };

    render(
      <AuthContext.Provider value={mockAuthValue}>
        <MemoryRouter initialEntries={['/protected']}>
          <Routes>
            <Route
              path="/protected"
              element={
                <ProtectedRoute>
                  <div>Protected Content</div>
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<div data-testid="login-page">Login Page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    expect(screen.getByTestId('login-page')).toBeInTheDocument();
  });
});
