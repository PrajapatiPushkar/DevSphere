import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { authService } from '../services/authService';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Alert } from '../components/common/Alert';
import { Terminal, Mail, Lock, Eye, EyeOff, ArrowRight } from 'lucide-react';
import { ApiError } from '../types';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  const { isAuthenticated, login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/dashboard';

  // Redirect authenticated user if already logged in
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const validate = (): boolean => {
    const errors: { email?: string; password?: string } = {};

    if (!email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Must be a valid email address';
    }

    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!validate()) return;

    setIsLoading(true);

    try {
      // 1. Authenticate with auth-service
      const loginRes = await authService.login({ email: email.trim(), password });
      
      // Store token in localStorage for subsequent requests
      localStorage.setItem('devsphere_token', loginRes.accessToken);

      // 2. Fetch current user profile from user-service
      let currentUser;
      try {
        currentUser = await authService.getCurrentUser();
      } catch {
        // Fallback user state if user-service is expanding
        currentUser = {
          id: 1,
          email: email.trim(),
          displayName: email.split('@')[0],
          role: 'USER',
        };
      }

      // Update global AuthContext state
      login(loginRes.accessToken, currentUser);
      showToast('Signed in successfully!', 'success', 'Welcome');
      navigate(from, { replace: true });
    } catch (err) {
      const apiErr = err as ApiError;
      const errorMessage = apiErr.message || 'Invalid email or password';
      setFormError(errorMessage);
      showToast(errorMessage, 'error', 'Authentication Failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center p-4 sm:p-6 relative selection:bg-brand-500 selection:text-white">
      {/* Background Ambient Glow */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-brand-600/15 rounded-full blur-3xl pointer-events-none" />

      {/* Header Logo */}
      <Link to="/" className="flex items-center gap-3 mb-8">
        <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center shadow-glow">
          <Terminal className="w-5 h-5 text-white" />
        </div>
        <span className="text-2xl font-bold tracking-tight text-slate-100">DevSphere</span>
      </Link>

      {/* Auth Card */}
      <Card glass className="max-w-md w-full border border-slate-800">
        <CardHeader>
          <CardTitle className="text-xl">Welcome back 👋</CardTitle>
          <CardDescription>Sign in to your DevSphere developer account</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {formError && (
              <Alert type="error" title="Authentication Error">
                {formError}
              </Alert>
            )}

            <Input
              label="Email Address"
              type="email"
              placeholder="developer@devsphere.io"
              required
              disabled={isLoading}
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (fieldErrors.email) setFieldErrors((prev) => ({ ...prev, email: undefined }));
              }}
              error={fieldErrors.email}
              leftIcon={<Mail className="w-4 h-4" />}
            />

            <Input
              label="Password"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••••••"
              required
              disabled={isLoading}
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: undefined }));
              }}
              error={fieldErrors.password}
              leftIcon={<Lock className="w-4 h-4" />}
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-slate-400 hover:text-slate-200 focus:outline-none"
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
            />

            <div className="flex items-center justify-between pt-1 text-xs">
              <span className="text-slate-400">Restricted session access</span>
              <button
                type="button"
                onClick={() =>
                  showToast(
                    'Password recovery API is intentionally deferred in backend architecture',
                    'info',
                    'Notice'
                  )
                }
                className="text-brand-400 hover:underline font-medium"
              >
                Forgot password?
              </button>
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full mt-2"
              isLoading={isLoading}
              disabled={isLoading}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Sign In
            </Button>
          </form>
        </CardContent>
        <CardFooter className="justify-center text-xs text-slate-400">
          <span>Don't have an account?</span>
          <Link to="/register" className="ml-1 text-brand-400 font-semibold hover:underline">
            Create an account
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
};
