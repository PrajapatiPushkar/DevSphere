import React from 'react';
import { Card } from '../ui/Card';
import { Task } from '../../types';
import { CheckCircle2, PlusCircle, RefreshCw, Activity, Calendar } from 'lucide-react';
import { EmptyState } from '../common/EmptyState';

export interface RecentActivityProps {
  tasks: Task[];
}

interface ActivityItem {
  id: string;
  type: 'completed' | 'created' | 'updated';
  title: string;
  timestamp: string;
  formattedTime: string;
}

export const RecentActivity: React.FC<RecentActivityProps> = ({ tasks }) => {
  // Derive recent activities from tasks list sorted by date
  const activities: ActivityItem[] = [];

  tasks.forEach((t) => {
    if (t.completedAt || t.status === 'COMPLETED') {
      activities.push({
        id: `completed-${t.id}`,
        type: 'completed',
        title: `Completed "${t.title}"`,
        timestamp: t.completedAt || t.updatedAt || t.createdAt || new Date().toISOString(),
        formattedTime: formatRelativeTime(t.completedAt || t.updatedAt || t.createdAt),
      });
    } else if (t.createdAt) {
      activities.push({
        id: `created-${t.id}`,
        type: 'created',
        title: `Created task "${t.title}"`,
        timestamp: t.createdAt,
        formattedTime: formatRelativeTime(t.createdAt),
      });
    } else if (t.updatedAt && t.updatedAt !== t.createdAt) {
      activities.push({
        id: `updated-${t.id}`,
        type: 'updated',
        title: `Updated "${t.title}"`,
        timestamp: t.updatedAt,
        formattedTime: formatRelativeTime(t.updatedAt),
      });
    }
  });

  // Sort activities newest first and limit to 5
  activities.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  const recentItems = activities.slice(0, 5);

  return (
    <Card glass className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20 text-brand-400">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100 tracking-tight">Recent Activity</h3>
            <p className="text-xs text-slate-400">Latest actions and task updates</p>
          </div>
        </div>
      </div>

      {recentItems.length === 0 ? (
        <EmptyState
          icon={<Calendar className="w-8 h-8 text-slate-500" />}
          title="No recent activity"
          description="Your recent task creations and updates will appear here."
          className="p-6 my-2 border-slate-800/80 bg-slate-950/40"
        />
      ) : (
        <div className="space-y-3 pt-1">
          {recentItems.map((act) => (
            <div
              key={act.id}
              className="flex items-start gap-3 p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-slate-700 transition duration-150"
            >
              <div className="mt-0.5 shrink-0">
                {act.type === 'completed' && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
                {act.type === 'created' && <PlusCircle className="w-4 h-4 text-brand-400" />}
                {act.type === 'updated' && <RefreshCw className="w-4 h-4 text-amber-400" />}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-slate-200 truncate">{act.title}</p>
                <p className="text-[11px] text-slate-400 font-mono mt-0.5">{act.formattedTime}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
};

function formatRelativeTime(dateString?: string | null): string {
  if (!dateString) return 'Recently';
  const date = new Date(dateString);
  const now = new Date();
  const diffInMs = now.getTime() - date.getTime();
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
  const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

  if (diffInMinutes < 1) return 'Just now';
  if (diffInMinutes < 60) return `${diffInMinutes} minute${diffInMinutes === 1 ? '' : 's'} ago`;
  if (diffInHours < 24) return `${diffInHours} hour${diffInHours === 1 ? '' : 's'} ago`;
  if (diffInDays === 1) return 'Yesterday';
  if (diffInDays < 7) return `${diffInDays} days ago`;

  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}
