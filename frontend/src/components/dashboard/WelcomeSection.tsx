import React from 'react';
import { User } from '../../types';
import { Sparkles, Terminal } from 'lucide-react';
import { Badge } from '../ui/Badge';

export interface WelcomeSectionProps {
  user: User | null;
}

export const WelcomeSection: React.FC<WelcomeSectionProps> = ({ user }) => {
  const userName =
    user?.displayName ||
    (user?.firstName ? `${user.firstName}${user.lastName ? ' ' + user.lastName : ''}` : null) ||
    user?.email?.split('@')[0] ||
    'Developer';

  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-brand-950/40 to-slate-900 border border-slate-800/90 p-6 sm:p-8 shadow-xl">
      {/* Subtle background glow effect */}
      <div className="absolute -top-24 -right-24 w-72 h-72 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-brand-400 font-medium text-xs tracking-wider uppercase">
            <Sparkles className="w-4 h-4 text-brand-400 animate-pulse" />
            <span>Developer Workspace Dashboard</span>
            <Badge variant="brand" size="sm" className="ml-1">
              Live Telemetry
            </Badge>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 tracking-tight">
            Welcome back, {userName} <span className="inline-block animate-bounce">👋</span>
          </h1>

          <p className="text-sm text-slate-400 max-w-2xl leading-relaxed">
            Here's what's happening with your tasks and microservice activities today.
            {user?.headline ? ` ${user.headline}` : ''}
          </p>
        </div>

        <div className="flex items-center gap-3 self-start md:self-auto shrink-0 bg-slate-950/60 backdrop-blur-md px-4 py-3 rounded-xl border border-slate-800">
          <div className="w-9 h-9 rounded-lg bg-brand-600/20 border border-brand-500/30 flex items-center justify-center text-brand-400">
            <Terminal className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-200">DevSphere Engine</div>
            <div className="text-[10px] text-slate-400 font-mono">Microservice Cluster OK</div>
          </div>
        </div>
      </div>
    </div>
  );
};
