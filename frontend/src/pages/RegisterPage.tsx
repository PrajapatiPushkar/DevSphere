import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Checkbox } from '../components/ui/Checkbox';
import { Terminal, Mail, Lock, User, ArrowRight } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [firstName, setFirstName] = useState('Pushkar');
  const [lastName, setLastName] = useState('Prajapati');
  const [email, setEmail] = useState('pushkar@devsphere.io');
  const [password, setPassword] = useState('SecurePass123!');
  const [termsAccepted, setTermsAccepted] = useState(true);
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!termsAccepted) {
      showToast('Please accept the terms to proceed', 'warning');
      return;
    }

    setIsLoading(true);
    setTimeout(() => {
      login('devsphere-registered-token-sample', {
        id: 102,
        email,
        firstName,
        lastName,
        displayName: `${firstName} ${lastName}`,
        role: 'DEVELOPER',
      });
      setIsLoading(false);
      showToast('Account created successfully!', 'success', 'Welcome');
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

      {/* Registration Card */}
      <Card glass className="max-w-md w-full border border-slate-800">
        <CardHeader>
          <CardTitle className="text-xl">Create your account</CardTitle>
          <CardDescription>Get started with DevSphere microservices developer suite</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="First Name"
                placeholder="Pushkar"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                leftIcon={<User className="w-4 h-4" />}
              />
              <Input
                label="Last Name"
                placeholder="Prajapati"
                required
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
              />
            </div>
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
              helperText="At least 8 characters with numbers and symbols"
            />
            <Checkbox
              label="I agree to the Terms of Service & Privacy Policy"
              checked={termsAccepted}
              onChange={(e) => setTermsAccepted(e.target.checked)}
            />
            <Button
              type="submit"
              variant="primary"
              className="w-full mt-2"
              isLoading={isLoading}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Create Account
            </Button>
          </form>
        </CardContent>
        <CardFooter className="justify-center text-xs text-slate-400">
          <span>Already have an account?</span>
          <Link to="/login" className="ml-1 text-brand-400 font-semibold hover:underline">
            Sign In
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
};
