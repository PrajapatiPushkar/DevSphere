import React from 'react';
import { Card } from '../ui/Card';
import { Task } from '../../types';
import { Badge } from '../ui/Badge';
import { Link } from 'react-router-dom';
import { Calendar, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';
import { EmptyState } from '../common/EmptyState';

export interface UpcomingTasksProps {
  tasks: Task[];
  onCompleteTask?: (taskId: number) => void;
}

export const UpcomingTasks: React.FC<UpcomingTasksProps> = ({ tasks, onCompleteTask }) => {
  // Filter incomplete tasks (TODO, IN_PROGRESS)
  const incompleteTasks = tasks.filter(
    (t) => t.status === 'TODO' || t.status === 'IN_PROGRESS'
  );

  // Sort: tasks with due date first (nearest first), then tasks without due date
  incompleteTasks.sort((a, b) => {
    if (a.dueDate && b.dueDate) {
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    }
    if (a.dueDate) return -1;
    if (b.dueDate) return 1;
    return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime();
  });

  const upcomingList = incompleteTasks.slice(0, 5);

  const priorityVariants: Record<string, 'danger' | 'warning' | 'info' | 'neutral'> = {
    URGENT: 'danger',
    HIGH: 'warning',
    MEDIUM: 'info',
    LOW: 'neutral',
  };

  return (
    <Card glass className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100 tracking-tight">Upcoming Tasks</h3>
            <p className="text-xs text-slate-400">Tasks needing attention next</p>
          </div>
        </div>

        <Link
          to="/tasks"
          className="flex items-center gap-1 text-xs font-semibold text-brand-400 hover:text-brand-300 transition group"
        >
          <span>View all tasks</span>
          <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
        </Link>
      </div>

      {upcomingList.length === 0 ? (
        <EmptyState
          icon={<CheckCircle2 className="w-8 h-8 text-emerald-400" />}
          title="You're all caught up 🎉"
          description="No upcoming pending tasks found."
          className="p-6 my-2 border-slate-800/80 bg-slate-950/40"
        />
      ) : (
        <div className="space-y-3 pt-1">
          {upcomingList.map((task) => (
            <div
              key={task.id}
              className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-slate-700 transition duration-150"
            >
              <div className="space-y-1 min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-100 truncate">{task.title}</span>
                  {task.overdue && (
                    <span className="inline-flex items-center gap-1 text-[10px] text-rose-400 font-bold bg-rose-500/10 px-1.5 py-0.5 rounded border border-rose-500/20">
                      <AlertCircle className="w-3 h-3" /> Overdue
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-3 text-[11px] text-slate-400">
                  <span className="font-mono">
                    Due: {formatDueDate(task.dueDate)}
                  </span>
                  <span>•</span>
                  <span className="font-mono text-slate-500">ID: TASK-{task.id}</span>
                </div>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-auto shrink-0">
                <Badge variant={priorityVariants[task.priority] || 'neutral'} size="sm">
                  {task.priority}
                </Badge>
                {onCompleteTask && (
                  <button
                    onClick={() => onCompleteTask(task.id)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-400 hover:bg-emerald-500/10 border border-transparent hover:border-emerald-500/20 transition"
                    title="Mark as completed"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
};

function formatDueDate(dueDateStr?: string | null): string {
  if (!dueDateStr) return 'No due date';
  const dueDate = new Date(dueDateStr);
  const now = new Date();
  const diffInDays = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

  if (diffInDays === 0) return 'Today';
  if (diffInDays === 1) return 'Tomorrow';
  if (diffInDays < 0) return `${Math.abs(diffInDays)} day${Math.abs(diffInDays) === 1 ? '' : 's'} ago`;
  if (diffInDays < 7) return `In ${diffInDays} days`;

  return dueDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}
