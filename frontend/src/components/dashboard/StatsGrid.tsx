import React from 'react';
import { StatCard } from './StatCard';
import { DashboardStats } from '../../types';
import { CheckSquare, Clock, CheckCircle2, AlertTriangle } from 'lucide-react';

export interface StatsGridProps {
  stats: DashboardStats;
}

export const StatsGrid: React.FC<StatsGridProps> = ({ stats }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
      <StatCard
        title="Total Tasks"
        value={stats.total}
        subtitle="All active & archived tasks"
        icon={<CheckSquare className="w-5 h-5" />}
        variant="brand"
      />
      <StatCard
        title="Pending Tasks"
        value={stats.pending}
        subtitle="In Progress & To-Do"
        icon={<Clock className="w-5 h-5" />}
        variant="warning"
      />
      <StatCard
        title="Completed Tasks"
        value={stats.completed}
        subtitle="Finished successfully"
        icon={<CheckCircle2 className="w-5 h-5" />}
        variant="success"
      />
      <StatCard
        title="Overdue Tasks"
        value={stats.overdue}
        subtitle={stats.overdue > 0 ? "Requires urgent attention" : "All deadlines on track"}
        icon={<AlertTriangle className="w-5 h-5" />}
        variant={stats.overdue > 0 ? "danger" : "brand"}
      />
    </div>
  );
};
