import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Checkbox } from '../components/ui/Checkbox';
import { Terminal, Mail, Lock, ArrowRight } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('engineer@devsphere.io');
  const [password, setPassword] = useState('Password123!');
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    setTimeout(() => {
      login('devsphere-sample-jwt-token', {
        id: 101,
        email,
        displayName: 'Pushkar Prajapati',
        role: 'SOFTWARE_ENGINEER',
      });
      setIsLoading(false);
      showToast('Welcome back to DevSphere Console', 'success', 'Authenticated');
      navigate('/dashboard');
    }, 800);
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
          <CardTitle className="text-xl">Sign in to DevSphere</CardTitle>
          <CardDescription>Enter your credentials to access your developer console</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Work Email"
              type="email"
              placeholder="you@company.com"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              leftIcon={<Mail className="w-4 h-4" />}
            />
            <Input
              label="Password"
              type="password"
              placeholder="••••••••••••"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              leftIcon={<Lock className="w-4 h-4" />}
              helperText="Must be at least 8 characters"
            />
            <div className="flex items-center justify-between pt-1">
              <Checkbox
                label="Remember session"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
              />
              <button
                type="button"
                onClick={() => showToast('Password reset link sent to registered email', 'info')}
                className="text-xs text-brand-400 hover:underline font-medium"
              >
                Forgot password?
              </button>
            </div>
            <Button
              type="submit"
              variant="primary"
              className="w-full mt-2"
              isLoading={isLoading}
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
