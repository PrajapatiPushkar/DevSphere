import React from 'react';
import { Task } from '../../types';
import { Badge } from '../ui/Badge';
import { Dropdown } from '../ui/Dropdown';
import {
  MoreVertical,
  CheckCircle2,
  Play,
  RotateCcw,
  Ban,
  Trash2,
  Edit3,
  AlertCircle,
  Calendar,
  Clock,
} from 'lucide-react';
import { cn } from '../../utils/cn';

export interface TaskTableProps {
  tasks: Task[];
  onEdit: (task: Task) => void;
  onComplete: (taskId: number) => void;
  onStart: (taskId: number) => void;
  onReopen: (taskId: number) => void;
  onCancel: (taskId: number) => void;
  onDelete: (task: Task) => void;
}

export const TaskTable: React.FC<TaskTableProps> = ({
  tasks,
  onEdit,
  onComplete,
  onStart,
  onReopen,
  onCancel,
  onDelete,
}) => {
  const priorityVariants: Record<string, 'danger' | 'warning' | 'info' | 'neutral'> = {
    URGENT: 'danger',
    HIGH: 'warning',
    MEDIUM: 'info',
    LOW: 'neutral',
  };

  const statusVariants: Record<string, 'success' | 'warning' | 'info' | 'danger' | 'neutral'> = {
    COMPLETED: 'success',
    IN_PROGRESS: 'warning',
    TODO: 'info',
    CANCELLED: 'danger',
    ARCHIVED: 'neutral',
  };

  return (
    <div className="space-y-3">
      {/* Desktop Table View */}
      <div className="hidden lg:block border border-slate-800 rounded-2xl bg-slate-900/60 overflow-hidden shadow-xl">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="bg-slate-950/80 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
              <th className="py-3.5 px-4">Task Details</th>
              <th className="py-3.5 px-4">Priority</th>
              <th className="py-3.5 px-4">Status</th>
              <th className="py-3.5 px-4">Due Date</th>
              <th className="py-3.5 px-4">Created</th>
              <th className="py-3.5 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/80 text-slate-200">
            {tasks.map((task) => (
              <tr
                key={task.id}
                className="hover:bg-slate-800/40 transition duration-150 group"
              >
                <td className="py-4 px-4 max-w-sm">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-100 text-sm group-hover:text-brand-300 transition">
                        {task.title}
                      </span>
                      {task.overdue && (
                        <span className="inline-flex items-center gap-1 text-[10px] text-rose-400 font-bold bg-rose-500/10 px-1.5 py-0.5 rounded border border-rose-500/20">
                          <AlertCircle className="w-3 h-3" /> Overdue
                        </span>
                      )}
                    </div>
                    {task.description && (
                      <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">
                        {task.description}
                      </p>
                    )}
                    <p className="text-[10px] font-mono text-slate-500">ID: TASK-{task.id}</p>
                  </div>
                </td>

                <td className="py-4 px-4 whitespace-nowrap">
                  <Badge variant={priorityVariants[task.priority] || 'neutral'} size="sm">
                    {task.priority}
                  </Badge>
                </td>

                <td className="py-4 px-4 whitespace-nowrap">
                  <Badge variant={statusVariants[task.status] || 'neutral'} size="sm" dot>
                    {task.status.replace('_', ' ')}
                  </Badge>
                </td>

                <td className="py-4 px-4 whitespace-nowrap">
                  <span className={cn('font-mono text-xs', task.overdue ? 'text-rose-400 font-semibold' : 'text-slate-400')}>
                    {formatDate(task.dueDate)}
                  </span>
                </td>

                <td className="py-4 px-4 whitespace-nowrap text-slate-400 font-mono text-xs">
                  {formatDate(task.createdAt)}
                </td>

                <td className="py-4 px-4 text-right whitespace-nowrap">
                  <Dropdown
                    align="right"
                    trigger={
                      <button
                        className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition border border-transparent hover:border-slate-700"
                        title="Task actions"
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>
                    }
                    items={getTaskActionItems(task, {
                      onEdit,
                      onComplete,
                      onStart,
                      onReopen,
                      onCancel,
                      onDelete,
                    })}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile & Tablet Card Layout */}
      <div className="lg:hidden space-y-3">
        {tasks.map((task) => (
          <div
            key={task.id}
            className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3 shadow-md"
          >
            <div className="flex items-start justify-between gap-2">
              <div className="space-y-1 min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-bold text-slate-100 text-sm">{task.title}</span>
                  {task.overdue && (
                    <span className="inline-flex items-center gap-1 text-[10px] text-rose-400 font-bold bg-rose-500/10 px-1.5 py-0.5 rounded border border-rose-500/20">
                      <AlertCircle className="w-3 h-3" /> Overdue
                    </span>
                  )}
                </div>
                <p className="text-[10px] font-mono text-slate-500">ID: TASK-{task.id}</p>
              </div>

              <Dropdown
                align="right"
                trigger={
                  <button className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition">
                    <MoreVertical className="w-4 h-4" />
                  </button>
                }
                items={getTaskActionItems(task, {
                  onEdit,
                  onComplete,
                  onStart,
                  onReopen,
                  onCancel,
                  onDelete,
                })}
              />
            </div>

            {task.description && (
              <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">
                {task.description}
              </p>
            )}

            <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-800/80">
              <div className="flex items-center gap-2">
                <Badge variant={statusVariants[task.status] || 'neutral'} size="sm" dot>
                  {task.status.replace('_', ' ')}
                </Badge>
                <Badge variant={priorityVariants[task.priority] || 'neutral'} size="sm">
                  {task.priority}
                </Badge>
              </div>

              <div className="text-[11px] font-mono text-slate-400 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-slate-500" />
                <span>Due: {formatDate(task.dueDate)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

function formatDate(dateStr?: string | null): string {
  if (!dateStr) return 'No due date';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return dateStr;
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function getTaskActionItems(
  task: Task,
  handlers: {
    onEdit: (task: Task) => void;
    onComplete: (id: number) => void;
    onStart: (id: number) => void;
    onReopen: (id: number) => void;
    onCancel: (id: number) => void;
    onDelete: (task: Task) => void;
  }
) {
  const items = [];

  items.push({
    id: 'edit',
    label: 'Edit Task',
    icon: <Edit3 className="w-4 h-4 text-slate-400" />,
    onClick: () => handlers.onEdit(task),
  });

  if (task.status === 'TODO') {
    items.push({
      id: 'start',
      label: 'Start Task',
      icon: <Play className="w-4 h-4 text-amber-400" />,
      onClick: () => handlers.onStart(task.id),
    });
  }

  if (task.status === 'TODO' || task.status === 'IN_PROGRESS') {
    items.push({
      id: 'complete',
      label: 'Mark Completed',
      icon: <CheckCircle2 className="w-4 h-4 text-emerald-400" />,
      onClick: () => handlers.onComplete(task.id),
    });
  }

  if (task.status === 'COMPLETED' || task.status === 'CANCELLED') {
    items.push({
      id: 'reopen',
      label: 'Reopen Task',
      icon: <RotateCcw className="w-4 h-4 text-brand-400" />,
      onClick: () => handlers.onReopen(task.id),
    });
  }

  if (task.status !== 'COMPLETED' && task.status !== 'CANCELLED') {
    items.push({
      id: 'cancel',
      label: 'Cancel Task',
      icon: <Ban className="w-4 h-4 text-slate-400" />,
      onClick: () => handlers.onCancel(task.id),
    });
  }

  items.push({
    id: 'delete',
    label: 'Delete Task',
    icon: <Trash2 className="w-4 h-4" />,
    onClick: () => handlers.onDelete(task),
    danger: true,
  });

  return items;
}
