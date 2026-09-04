import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../ui/Button';
import { Dropdown } from '../ui/Dropdown';
import { Badge } from '../ui/Badge';
import { Menu, Bell, User as UserIcon, LogOut, Terminal, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

export interface HeaderProps {
  onToggleSidebar: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleSidebar }) => {
  const { user, isAuthenticated, logout } = useAuth();
  const { showToast } = useToast();

  const userMenuItems = [
    {
      id: 'profile',
      label: 'Developer Profile',
      icon: <UserIcon className="w-4 h-4 text-slate-400" />,
      onClick: () => showToast('Profile details available in upcoming module', 'info'),
    },
    {
      id: 'logout',
      label: 'Sign Out',
      icon: <LogOut className="w-4 h-4" />,
      onClick: () => {
        logout();
        showToast('Signed out successfully', 'success');
      },
      danger: true,
    },
  ];

  return (
    <header className="sticky top-0 z-30 h-16 bg-slate-950/80 backdrop-blur-md border-b border-slate-800/80 px-4 sm:px-6 flex items-center justify-between">
      {/* Left side: Mobile Toggle & Brand */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="lg:hidden p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
        >
          <Menu className="w-5 h-5" />
        </button>

        <Link to="/" className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center shadow-glow">
            <Terminal className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="text-base font-bold text-slate-100 tracking-tight">DevSphere</span>
            <span className="hidden sm:inline-block ml-2 text-[10px] bg-slate-800 text-brand-300 font-mono px-2 py-0.5 rounded-full border border-slate-700">
              v1.0-k8s
            </span>
          </div>
        </Link>
      </div>

      {/* Right side: Status, Quick Actions & Auth */}
      <div className="flex items-center gap-3">
        <Badge variant="success" dot className="hidden md:inline-flex">
          Cluster Operational
        </Badge>

        <button
          onClick={() => showToast('No new notifications', 'info')}
          className="p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition relative"
        >
          <Bell className="w-5 h-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-brand-500 rounded-full" />
        </button>

        {isAuthenticated ? (
          <Dropdown
            align="right"
            trigger={
              <button className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-slate-800/80 transition text-left border border-transparent hover:border-slate-700">
                <div className="w-8 h-8 rounded-lg bg-brand-600/20 text-brand-400 font-semibold flex items-center justify-center text-xs border border-brand-500/30">
                  {user?.displayName?.substring(0, 2).toUpperCase() || 'DS'}
                </div>
                <div className="hidden sm:block text-xs">
                  <p className="font-semibold text-slate-200">{user?.displayName}</p>
                  <p className="text-[10px] text-slate-400">{user?.email}</p>
                </div>
              </button>
            }
            items={userMenuItems}
          />
        ) : (
          <div className="flex items-center gap-2">
            <Link to="/login">
              <Button variant="ghost" size="sm">
                Sign In
              </Button>
            </Link>
            <Link to="/register">
              <Button variant="primary" size="sm">
                Get Started
              </Button>
            </Link>
          </div>
        )}
      </div>
    </header>
  );
};
