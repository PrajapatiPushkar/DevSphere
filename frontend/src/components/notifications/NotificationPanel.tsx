import React, { useState } from 'react';
import { NotificationItemData } from '../../types/notificationTypes';
import { NotificationItem } from './NotificationItem';
import { Skeleton } from '../common/Skeleton';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Bell, CheckCheck, RefreshCw, X, Sparkles } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '../../utils/cn';

export interface NotificationPanelProps {
  notifications: NotificationItemData[];
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
  onRefresh: () => void;
  onMarkRead: (notification: NotificationItemData) => void;
  onMarkAllRead: () => void;
}

export const NotificationPanel: React.FC<NotificationPanelProps> = ({
  notifications,
  isLoading,
  error,
  onClose,
  onRefresh,
  onMarkRead,
  onMarkAllRead,
}) => {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const filteredNotifications = notifications.filter((n) => {
    if (filter === 'unread') return !n.isRead;
    return true;
  });

  const handleItemClick = (notification: NotificationItemData) => {
    if (!notification.isRead) {
      onMarkRead(notification);
    }
    if (notification.targetUrl) {
      navigate(notification.targetUrl);
      onClose();
    }
  };

  const handleMarkReadClick = (e: React.MouseEvent, notification: NotificationItemData) => {
    e.stopPropagation();
    onMarkRead(notification);
  };

  return (
    <div className="w-full sm:w-96 bg-slate-950 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-150">
      {/* Header Bar */}
      <div className="px-4 py-3.5 border-b border-slate-800 flex items-center justify-between bg-slate-900/60">
        <div className="flex items-center gap-2">
          <Bell className="w-4 h-4 text-brand-400" />
          <h4 className="text-sm font-bold text-slate-100">Notifications</h4>
          {unreadCount > 0 && (
            <Badge variant="brand" size="sm" className="font-mono">
              {unreadCount} New
            </Badge>
          )}
        </div>

        <div className="flex items-center gap-1">
          {unreadCount > 0 && (
            <button
              onClick={onMarkAllRead}
              title="Mark all as read"
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 text-xs transition flex items-center gap-1 font-medium"
            >
              <CheckCheck className="w-4 h-4 text-brand-400" />
              <span className="hidden sm:inline">Mark read</span>
            </button>
          )}
          <button
            onClick={onRefresh}
            title="Refresh notifications"
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <RefreshCw className={cn('w-4 h-4', isLoading && 'animate-spin')} />
          </button>
          <button
            onClick={onClose}
            title="Close panel"
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="px-4 py-2 border-b border-slate-800/80 bg-slate-950 flex items-center gap-2">
        <button
          onClick={() => setFilter('all')}
          className={cn(
            'px-3 py-1 rounded-lg text-xs font-medium transition',
            filter === 'all'
              ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
          )}
        >
          All ({notifications.length})
        </button>
        <button
          onClick={() => setFilter('unread')}
          className={cn(
            'px-3 py-1 rounded-lg text-xs font-medium transition',
            filter === 'unread'
              ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
          )}
        >
          Unread ({unreadCount})
        </button>
      </div>

      {/* Notification List Container */}
      <div className="p-3 overflow-y-auto space-y-2 flex-1 min-h-[220px]">
        {isLoading ? (
          <div className="space-y-2 p-1" data-testid="notification-skeleton">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="p-3 rounded-xl border border-slate-800 space-y-2 bg-slate-900/40">
                <Skeleton className="h-4 w-32 rounded" />
                <Skeleton className="h-3 w-48 rounded" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="p-6 text-center space-y-3">
            <p className="text-xs text-rose-400">{error}</p>
            <Button variant="ghost" size="sm" onClick={onRefresh}>
              Try Again
            </Button>
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div className="p-8 text-center space-y-2">
            <div className="w-10 h-10 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center mx-auto text-slate-500">
              <Sparkles className="w-5 h-5 text-brand-400" />
            </div>
            <h5 className="text-xs font-semibold text-slate-300">You're all caught up 🎉</h5>
            <p className="text-[11px] text-slate-500">No new notifications at this time.</p>
          </div>
        ) : (
          filteredNotifications.map((notif) => (
            <NotificationItem
              key={notif.id}
              notification={notif}
              onItemClick={handleItemClick}
              onMarkReadClick={handleMarkReadClick}
            />
          ))
        )}
      </div>

      {/* Footer Info */}
      <div className="px-4 py-2 border-t border-slate-800/80 bg-slate-950 text-[10px] text-slate-500 flex justify-between items-center">
        <span>DevSphere Telemetry</span>
        <button
          onClick={() => {
            navigate('/activity');
            onClose();
          }}
          className="text-brand-400 hover:underline font-mono"
        >
          View Activity Log →
        </button>
      </div>
    </div>
  );
};
