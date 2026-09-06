import { taskService } from './taskService';
import { userService } from './userService';
import { NotificationItemData, ActivityItemData } from '../types/notificationTypes';
import { Task, User } from '../types';

const READ_NOTIFICATIONS_KEY = 'devsphere_read_notifications';

const getReadIds = (): Set<string> => {
  try {
    const saved = localStorage.getItem(READ_NOTIFICATIONS_KEY);
    return saved ? new Set(JSON.parse(saved)) : new Set();
  } catch {
    return new Set();
  }
};

const saveReadIds = (readIds: Set<string>): void => {
  try {
    localStorage.setItem(READ_NOTIFICATIONS_KEY, JSON.stringify(Array.from(readIds)));
  } catch {
    // ignore storage errors
  }
};

export const activityService = {
  async getNotifications(): Promise<NotificationItemData[]> {
    const readIds = getReadIds();
    const notifications: NotificationItemData[] = [];

    try {
      const taskPage = await taskService.getTasks({ page: 0, size: 50 });
      const tasks: Task[] = taskPage.content || [];

      tasks.forEach((task) => {
        // 1. Overdue Task Notification
        if (task.overdue || (task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'COMPLETED')) {
          const id = `notif-overdue-${task.id}`;
          notifications.push({
            id,
            category: 'TASK_OVERDUE',
            title: 'Task Overdue Notice',
            message: `Task "${task.title}" is overdue and requires attention.`,
            timestamp: task.dueDate || task.createdAt || new Date().toISOString(),
            isRead: readIds.has(id),
            targetUrl: '/tasks',
            entityId: task.id,
          });
        }

        // 2. Task Completed Notification
        if (task.status === 'COMPLETED' && task.completedAt) {
          const id = `notif-completed-${task.id}`;
          notifications.push({
            id,
            category: 'TASK_COMPLETED',
            title: 'Task Completed',
            message: `Task "${task.title}" was marked as completed.`,
            timestamp: task.completedAt,
            isRead: readIds.has(id),
            targetUrl: '/tasks',
            entityId: task.id,
          });
        }

        // 3. Task Created Notification
        if (task.createdAt) {
          const id = `notif-created-${task.id}`;
          notifications.push({
            id,
            category: 'TASK_CREATED',
            title: 'New Task Created',
            message: `Task "${task.title}" was logged to task management.`,
            timestamp: task.createdAt,
            isRead: readIds.has(id),
            targetUrl: '/tasks',
            entityId: task.id,
          });
        }
      });
    } catch {
      // Fallback if task service is offline
    }

    try {
      const profile: User = await userService.getMyProfile();
      if (profile.updatedAt) {
        const id = `notif-profile-${profile.userId || profile.id || 'me'}`;
        notifications.push({
          id,
          category: 'PROFILE_UPDATED',
          title: 'Developer Profile Updated',
          message: `Profile details for ${profile.displayName || 'developer'} were updated.`,
          timestamp: profile.updatedAt,
          isRead: readIds.has(id),
          targetUrl: '/profile',
        });
      }
    } catch {
      // Fallback if user profile service is offline
    }

    // Sort chronologically (newest first)
    return notifications.sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );
  },

  async getActivityTimeline(): Promise<ActivityItemData[]> {
    const activities: ActivityItemData[] = [];

    try {
      const taskPage = await taskService.getTasks({ page: 0, size: 50 });
      const tasks: Task[] = taskPage.content || [];

      tasks.forEach((task) => {
        // Task Created Event
        if (task.createdAt) {
          activities.push({
            id: `act-create-${task.id}`,
            type: 'TASK_CREATED',
            title: `Created task "${task.title}"`,
            description: task.description || `Priority: ${task.priority}`,
            timestamp: task.createdAt,
            targetUrl: '/tasks',
            entityId: task.id,
          });
        }

        // Task Completed Event
        if (task.status === 'COMPLETED' && task.completedAt) {
          activities.push({
            id: `act-complete-${task.id}`,
            type: 'TASK_COMPLETED',
            title: `Completed task "${task.title}"`,
            description: `Finished with priority ${task.priority}`,
            timestamp: task.completedAt,
            targetUrl: '/tasks',
            entityId: task.id,
          });
        }

        // Task Updated Event (if updated after creation and not completed)
        if (
          task.updatedAt &&
          task.createdAt &&
          new Date(task.updatedAt).getTime() > new Date(task.createdAt).getTime() + 1000 &&
          task.status !== 'COMPLETED'
        ) {
          activities.push({
            id: `act-update-${task.id}`,
            type: 'TASK_UPDATED',
            title: `Updated task "${task.title}"`,
            description: `Current status: ${task.status}`,
            timestamp: task.updatedAt,
            targetUrl: '/tasks',
            entityId: task.id,
          });
        }
      });
    } catch {
      // Fallback
    }

    try {
      const profile: User = await userService.getMyProfile();
      if (profile.updatedAt) {
        activities.push({
          id: `act-profile-${profile.userId || profile.id || 'me'}`,
          type: 'PROFILE_UPDATED',
          title: 'Updated Developer Profile Information',
          description: `Role: ${profile.currentRole || 'Software Engineer'}`,
          timestamp: profile.updatedAt,
          targetUrl: '/profile',
        });
      }
    } catch {
      // Fallback
    }

    // Sort chronologically (newest first)
    return activities.sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );
  },

  markAsRead(id: string): void {
    const readIds = getReadIds();
    readIds.add(id);
    saveReadIds(readIds);
  },

  markAllAsRead(ids: string[]): void {
    const readIds = getReadIds();
    ids.forEach((id) => readIds.add(id));
    saveReadIds(readIds);
  },
};
