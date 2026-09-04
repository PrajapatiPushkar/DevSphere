import React from 'react';
import { NavLink } from 'react-router-dom';
import { cn } from '../../utils/cn';
import { LayoutDashboard, CheckSquare, FileText, Activity, Settings, Code, Sparkles, X } from 'lucide-react';

export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const navItems = [
    { label: 'Overview', to: '/dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
    { label: 'Task Management', to: '/tasks', icon: <CheckSquare className="w-4 h-4" />, badge: 'Lesson 72' },
    { label: 'Resume Compiler', to: '/resumes', icon: <FileText className="w-4 h-4" />, badge: 'Lesson 74' },
    { label: 'Observability', to: '/observability', icon: <Activity className="w-4 h-4" />, badge: 'Prometheus' },
    { label: 'API Playground', to: '/api-docs', icon: <Code className="w-4 h-4" /> },
    { label: 'Settings', to: '/settings', icon: <Settings className="w-4 h-4" /> },
  ];

  return (
    <>
      {/* Mobile Backdrop Overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/80 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={cn(
          'fixed lg:static top-16 bottom-0 left-0 z-40 w-64 bg-slate-950 border-r border-slate-800/80 flex flex-col justify-between transition-transform duration-200 ease-in-out',
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        )}
      >
        <div className="p-4 space-y-6 overflow-y-auto">
          {/* Mobile Close Button */}
          <div className="flex lg:hidden items-center justify-between pb-2 border-b border-slate-800">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Navigation</span>
            <button onClick={onClose} className="p-1 text-slate-400 hover:text-slate-200">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Navigation Links */}
          <nav className="space-y-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-medium transition-all duration-150',
                    isActive
                      ? 'bg-brand-600/15 text-brand-400 border border-brand-500/30 shadow-sm font-semibold'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                  )
                }
              >
                <div className="flex items-center gap-3">
                  {item.icon}
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span className="text-[9px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded font-mono border border-slate-700">
                    {item.badge}
                  </span>
                )}
              </NavLink>
            ))}
          </nav>

          {/* SaaS Microservices Feature Card */}
          <div className="p-4 rounded-2xl glass-card border border-brand-500/20 space-y-2">
            <div className="flex items-center gap-2 text-brand-400 text-xs font-semibold">
              <Sparkles className="w-4 h-4" />
              <span>Full-Stack Platform</span>
            </div>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              DevSphere microservices architecture powered by Spring Boot 3 & Kubernetes.
            </p>
          </div>
        </div>

        {/* Footer info */}
        <div className="p-4 border-t border-slate-800/80 text-[11px] text-slate-500 flex justify-between items-center">
          <span>DevSphere Frontend</span>
          <span className="font-mono text-slate-400">Lesson 71</span>
        </div>
      </aside>
    </>
  );
};
