import React from 'react';
import { Card } from '../ui/Card';
import { Skeleton } from '../common/Skeleton';
import { CheckCircle2, Clock, AlertCircle, BarChart2, Calendar } from 'lucide-react';

export interface ProfileStatsData {
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  overdueTasks: number;
  completionRate: number;
  accountAgeDays?: number;
}

export interface ProfileStatsProps {
  stats: ProfileStatsData | null;
  isLoading: boolean;
  userCreatedAt?: string;
}

export const ProfileStats: React.FC<ProfileStatsProps> = ({ stats, isLoading, userCreatedAt }) => {
  if (isLoading || !stats) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="profile-stats-skeleton">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i} className="p-5 space-y-3">
            <Skeleton className="h-4 w-24 rounded" />
            <Skeleton className="h-8 w-16 rounded" />
            <Skeleton className="h-3 w-32 rounded" />
          </Card>
        ))}
      </div>
    );
  }

  const calculateAccountAge = (): string => {
    if (!userCreatedAt) return 'Active Member';
    const created = new Date(userCreatedAt).getTime();
    if (isNaN(created)) return 'Active Member';
    const now = Date.now();
    const diffDays = Math.max(1, Math.floor((now - created) / (1000 * 60 * 60 * 24)));
    if (diffDays === 1) return '1 Day';
    if (diffDays < 30) return `${diffDays} Days`;
    const months = Math.floor(diffDays / 30);
    return months === 1 ? '1 Month' : `${months} Months`;
  };

  const statCards = [
    {
      title: 'Total Tasks',
      value: stats.totalTasks,
      subtitle: `${stats.pendingTasks} pending tasks remaining`,
      icon: <BarChart2 className="w-5 h-5 text-brand-400" />,
      badgeBg: 'bg-brand-500/10 border-brand-500/20 text-brand-400',
    },
    {
      title: 'Completed Tasks',
      value: stats.completedTasks,
      subtitle: `${stats.completionRate}% completion velocity`,
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-400" />,
      badgeBg: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
    },
    {
      title: 'Pending & Overdue',
      value: `${stats.pendingTasks} / ${stats.overdueTasks}`,
      subtitle: `${stats.overdueTasks} tasks require immediate attention`,
      icon: <Clock className="w-5 h-5 text-amber-400" />,
      badgeBg: 'bg-amber-500/10 border-amber-500/20 text-amber-400',
    },
    {
      title: 'Account Tenancy',
      value: calculateAccountAge(),
      subtitle: 'Platform registration duration',
      icon: <Calendar className="w-5 h-5 text-indigo-400" />,
      badgeBg: 'bg-indigo-500/10 border-indigo-500/20 text-indigo-400',
    },
  ];

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider">
          Account & Velocity Metrics
        </h3>
        <span className="text-[11px] text-slate-500 font-mono">Real-time telemetry</span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card, idx) => (
          <Card key={idx} className="p-5 hover:border-slate-700 transition">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-medium text-slate-400">{card.title}</span>
              <div className={`p-2 rounded-xl border ${card.badgeBg}`}>{card.icon}</div>
            </div>
            <div className="text-2xl font-bold text-slate-100 tracking-tight">{card.value}</div>
            <p className="text-[11px] text-slate-400 mt-1">{card.subtitle}</p>
          </Card>
        ))}
      </div>
    </div>
  );
};
