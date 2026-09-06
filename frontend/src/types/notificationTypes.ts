export type NotificationCategory = 'TASK_CREATED' | 'TASK_COMPLETED' | 'TASK_OVERDUE' | 'TASK_UPDATED' | 'PROFILE_UPDATED' | 'SYSTEM';

export interface NotificationItemData {
  id: string;
  category: NotificationCategory;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  targetUrl?: string;
  entityId?: string | number;
}

export type ActivityType = 'TASK_CREATED' | 'TASK_COMPLETED' | 'TASK_UPDATED' | 'PROFILE_UPDATED';

export interface ActivityItemData {
  id: string;
  type: ActivityType;
  title: string;
  description?: string;
  timestamp: string;
  targetUrl?: string;
  entityId?: string | number;
  metadata?: Record<string, any>;
}
