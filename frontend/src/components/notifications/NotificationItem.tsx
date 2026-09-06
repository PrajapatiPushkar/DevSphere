import React from 'react';
import { NotificationItemData } from '../../types/notificationTypes';
import { formatRelativeTime } from '../../utils/formatDate';
import { cn } from '../../utils/cn';
import { CheckCircle2, Clock, PlusCircle, RefreshCw, UserCheck, Check } from 'lucide-react';

export interface NotificationItemProps {
  notification: NotificationItemData;
  onItemClick: (notification: NotificationItemData) => void;
  onMarkReadClick: (e: React.MouseEvent, notification: NotificationItemData) => void;
}

export const NotificationItem: React.FC<NotificationItemProps> = ({
  notification,
  onItemClick,
  onMarkReadClick,
}) => {
  const getIcon = () => {
    switch (notification.category) {
      case 'TASK_COMPLETED':
        return <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />;
      case 'TASK_OVERDUE':
        return <Clock className="w-4 h-4 text-amber-400 shrink-0" />;
      case 'TASK_CREATED':
        return <PlusCircle className="w-4 h-4 text-brand-400 shrink-0" />;
      case 'TASK_UPDATED':
        return <RefreshCw className="w-4 h-4 text-indigo-400 shrink-0" />;
      case 'PROFILE_UPDATED':
        return <UserCheck className="w-4 h-4 text-sky-400 shrink-0" />;
      default:
        return <CheckCircle2 className="w-4 h-4 text-slate-400 shrink-0" />;
    }
  };

  return (
    <div
      onClick={() => onItemClick(notification)}
      className={cn(
        'group p-3.5 rounded-xl border transition-all duration-150 cursor-pointer flex items-start gap-3 relative',
        notification.isRead
          ? 'bg-slate-950/40 border-slate-800/60 hover:bg-slate-900/60'
          : 'bg-slate-900/90 border-brand-500/30 hover:border-brand-500/50 shadow-sm'
      )}
    >
      {/* Category Icon */}
      <div className="p-2 rounded-lg bg-slate-900 border border-slate-800 shrink-0 mt-0.5">
        {getIcon()}
      </div>

      {/* Content Body */}
      <div className="flex-1 min-w-0 pr-6 space-y-0.5">
        <div className="flex items-center justify-between gap-2">
          <h5 className="text-xs font-semibold text-slate-200 truncate">
            {notification.title}
          </h5>
          <span className="text-[10px] text-slate-500 font-mono shrink-0">
            {formatRelativeTime(notification.timestamp)}
          </span>
        </div>
        <p className="text-xs text-slate-400 leading-snug line-clamp-2">
          {notification.message}
        </p>
      </div>

      {/* Unread dot / Mark read action */}
      <div className="absolute top-3.5 right-3 flex items-center gap-1.5">
        {!notification.isRead && (
          <button
            onClick={(e) => onMarkReadClick(e, notification)}
            title="Mark as read"
            className="p-1 rounded-md text-slate-500 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <span className="w-2 h-2 rounded-full bg-brand-500 block group-hover:hidden" />
            <Check className="w-3.5 h-3.5 hidden group-hover:block text-brand-400" />
          </button>
        )}
      </div>
    </div>
  );
};
