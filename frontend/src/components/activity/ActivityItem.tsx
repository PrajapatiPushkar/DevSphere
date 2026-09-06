import React from 'react';
import { ActivityItemData } from '../../types/notificationTypes';
import { formatRelativeTime } from '../../utils/formatDate';
import { Badge } from '../ui/Badge';
import { PlusCircle, CheckCircle2, RefreshCw, UserCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export interface ActivityItemProps {
  activity: ActivityItemData;
  isLast?: boolean;
}

export const ActivityItem: React.FC<ActivityItemProps> = ({ activity, isLast = false }) => {
  const navigate = useNavigate();

  const getIconAndColor = () => {
    switch (activity.type) {
      case 'TASK_CREATED':
        return {
          icon: <PlusCircle className="w-4 h-4 text-brand-400" />,
          bg: 'bg-brand-500/10 border-brand-500/30',
          badgeVariant: 'brand' as const,
          label: 'Task Created',
        };
      case 'TASK_COMPLETED':
        return {
          icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" />,
          bg: 'bg-emerald-500/10 border-emerald-500/30',
          badgeVariant: 'success' as const,
          label: 'Task Completed',
        };
      case 'TASK_UPDATED':
        return {
          icon: <RefreshCw className="w-4 h-4 text-indigo-400" />,
          bg: 'bg-indigo-500/10 border-indigo-500/30',
          badgeVariant: 'info' as const,
          label: 'Task Updated',
        };
      case 'PROFILE_UPDATED':
        return {
          icon: <UserCheck className="w-4 h-4 text-sky-400" />,
          bg: 'bg-sky-500/10 border-sky-500/30',
          badgeVariant: 'neutral' as const,
          label: 'Profile Event',
        };
    }
  };

  const meta = getIconAndColor();

  return (
    <div className="relative flex items-start gap-4 group">
      {/* Vertical Timeline Line Connector */}
      {!isLast && (
        <span className="absolute left-5 top-10 bottom-0 w-0.5 bg-slate-800 group-hover:bg-slate-700 transition" />
      )}

      {/* Timeline Node Icon Circle */}
      <div
        className={`w-10 h-10 rounded-xl border ${meta.bg} flex items-center justify-center shrink-0 z-10 shadow-sm transition-transform duration-200 group-hover:scale-105`}
      >
        {meta.icon}
      </div>

      {/* Activity Card Body */}
      <div
        onClick={() => activity.targetUrl && navigate(activity.targetUrl)}
        className="flex-1 p-4 rounded-2xl bg-slate-900/60 hover:bg-slate-900 border border-slate-800/80 hover:border-slate-700 transition duration-150 cursor-pointer space-y-1.5"
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-semibold text-slate-100">{activity.title}</h4>
            <Badge variant={meta.badgeVariant} size="sm">
              {meta.label}
            </Badge>
          </div>
          <span className="text-xs text-slate-500 font-mono">
            {formatRelativeTime(activity.timestamp)}
          </span>
        </div>

        {activity.description && (
          <p className="text-xs text-slate-400 leading-relaxed">{activity.description}</p>
        )}
      </div>
    </div>
  );
};
