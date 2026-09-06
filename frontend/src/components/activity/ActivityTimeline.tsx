import React, { useState } from 'react';
import { ActivityItemData } from '../../types/notificationTypes';
import { ActivityItem } from './ActivityItem';
import { Skeleton } from '../common/Skeleton';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Activity, Search, Sparkles } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface ActivityTimelineProps {
  activities: ActivityItemData[];
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
}

export const ActivityTimeline: React.FC<ActivityTimelineProps> = ({
  activities,
  isLoading,
  error,
  onRetry,
}) => {
  const [filter, setFilter] = useState<'all' | 'tasks' | 'profile'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredActivities = activities.filter((act) => {
    // Filter type
    if (filter === 'tasks' && !act.type.startsWith('TASK_')) return false;
    if (filter === 'profile' && !act.type.startsWith('PROFILE_')) return false;

    // Search query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchTitle = act.title.toLowerCase().includes(q);
      const matchDesc = act.description ? act.description.toLowerCase().includes(q) : false;
      return matchTitle || matchDesc;
    }

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Controls & Filter Header */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80">
        <div className="flex items-center gap-1.5 overflow-x-auto">
          <button
            onClick={() => setFilter('all')}
            className={cn(
              'px-3.5 py-1.5 rounded-xl text-xs font-semibold transition',
              filter === 'all'
                ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900 border border-transparent'
            )}
          >
            All Activity ({activities.length})
          </button>
          <button
            onClick={() => setFilter('tasks')}
            className={cn(
              'px-3.5 py-1.5 rounded-xl text-xs font-semibold transition',
              filter === 'tasks'
                ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900 border border-transparent'
            )}
          >
            Tasks ({activities.filter((a) => a.type.startsWith('TASK_')).length})
          </button>
          <button
            onClick={() => setFilter('profile')}
            className={cn(
              'px-3.5 py-1.5 rounded-xl text-xs font-semibold transition',
              filter === 'profile'
                ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900 border border-transparent'
            )}
          >
            Profile ({activities.filter((a) => a.type.startsWith('PROFILE_')).length})
          </button>
        </div>

        <div className="w-full sm:w-64">
          <Input
            placeholder="Search activity timeline..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            leftIcon={<Search className="w-4 h-4" />}
            className="text-xs py-2"
          />
        </div>
      </div>

      {/* Timeline Content */}
      <div className="relative space-y-6 pt-2">
        {isLoading ? (
          <div className="space-y-4" data-testid="activity-skeleton">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="flex items-start gap-4">
                <Skeleton className="w-10 h-10 rounded-xl shrink-0" />
                <div className="flex-1 p-4 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-2">
                  <Skeleton className="h-4 w-48 rounded" />
                  <Skeleton className="h-3 w-32 rounded" />
                </div>
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 text-center space-y-3">
            <p className="text-sm text-rose-400">{error}</p>
            <Button variant="ghost" size="sm" onClick={onRetry}>
              Retry Loading Activity
            </Button>
          </div>
        ) : filteredActivities.length === 0 ? (
          <div className="p-12 rounded-2xl bg-slate-900/60 border border-slate-800 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center mx-auto text-slate-500">
              <Sparkles className="w-6 h-6 text-brand-400" />
            </div>
            <h4 className="text-sm font-semibold text-slate-200">No activity events found</h4>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Create or complete tasks to see your real-time developer activity stream populated here.
            </p>
          </div>
        ) : (
          filteredActivities.map((act, index) => (
            <ActivityItem
              key={act.id}
              activity={act}
              isLast={index === filteredActivities.length - 1}
            />
          ))
        )}
      </div>
    </div>
  );
};
