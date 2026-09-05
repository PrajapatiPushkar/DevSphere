import React, { useState, useEffect, useCallback } from 'react';
import { PageHeader } from '../components/layout/PageHeader';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Textarea } from '../components/ui/Textarea';
import { EmptyState } from '../components/common/EmptyState';
import { ErrorState } from '../components/common/ErrorState';
import { useToast } from '../context/ToastContext';
import { useAuth } from '../context/AuthContext';
import { taskService } from '../services/taskService';
import { Task, DashboardStats, TaskPriority } from '../types';
import { WelcomeSection } from '../components/dashboard/WelcomeSection';
import { StatsGrid } from '../components/dashboard/StatsGrid';
import { CompletionOverview } from '../components/dashboard/CompletionOverview';
import { RecentActivity } from '../components/dashboard/RecentActivity';
import { UpcomingTasks } from '../components/dashboard/UpcomingTasks';
import { QuickActions } from '../components/dashboard/QuickActions';
import { UserSummary } from '../components/dashboard/UserSummary';
import { DashboardSkeleton } from '../components/dashboard/DashboardSkeleton';
import { Plus, RefreshCw, CheckSquare } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isError, setIsError] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // New task form state
  const [taskTitle, setTaskTitle] = useState<string>('');
  const [taskDescription, setTaskDescription] = useState<string>('');
  const [taskPriority, setTaskPriority] = useState<TaskPriority>('MEDIUM');
  const [taskDueDate, setTaskDueDate] = useState<string>('');

  const fetchDashboardData = useCallback(async () => {
    setIsLoading(true);
    setIsError(false);
    setErrorMessage('');

    try {
      const pageResponse = await taskService.getTasks({
        page: 0,
        size: 50,
        sort: 'createdAt,desc',
      });
      setTasks(pageResponse.content || []);
    } catch (err: any) {
      setIsError(true);
      setErrorMessage(err?.message || 'Failed to connect to backend microservices.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  // Compute real dashboard statistics
  const stats: DashboardStats = React.useMemo(() => {
    const total = tasks.length;
    const pending = tasks.filter((t) => t.status === 'TODO' || t.status === 'IN_PROGRESS').length;
    const completed = tasks.filter((t) => t.status === 'COMPLETED').length;

    const overdue = tasks.filter((t) => {
      if (t.overdue) return true;
      if (!t.dueDate) return false;
      if (t.status === 'COMPLETED' || t.status === 'CANCELLED' || t.status === 'ARCHIVED') return false;
      return new Date(t.dueDate).getTime() < Date.now();
    }).length;

    const completionPercentage = total === 0 ? 0 : Math.round((completed / total) * 100);

    return {
      total,
      pending,
      completed,
      overdue,
      completionPercentage,
    };
  }, [tasks]);

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskTitle.trim()) {
      showToast('Task title is required', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await taskService.createTask({
        title: taskTitle.trim(),
        description: taskDescription.trim() || undefined,
        priority: taskPriority,
        dueDate: taskDueDate ? new Date(taskDueDate).toISOString() : undefined,
      });

      showToast('Task created successfully', 'success', 'Task Saved');
      setIsModalOpen(false);
      // Reset form
      setTaskTitle('');
      setTaskDescription('');
      setTaskPriority('MEDIUM');
      setTaskDueDate('');
      // Refresh dashboard data
      fetchDashboardData();
    } catch (err: any) {
      showToast(err?.message || 'Failed to create task', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCompleteTask = async (taskId: number) => {
    try {
      await taskService.completeTask(taskId);
      showToast(`Task-${taskId} marked as completed`, 'success');
      fetchDashboardData();
    } catch (err: any) {
      showToast(err?.message || 'Failed to complete task', 'error');
    }
  };

  return (
    <div className="space-y-8 pb-12">
      {/* Page Header */}
      <PageHeader
        title="Dashboard"
        description="DevSphere SaaS workspace overview, task execution statistics, and platform telemetry."
        breadcrumbs={[{ label: 'Console', href: '#' }, { label: 'Dashboard' }]}
        actions={
          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="sm"
              leftIcon={<RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />}
              onClick={fetchDashboardData}
              disabled={isLoading}
            >
              Refresh
            </Button>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Plus className="w-4 h-4" />}
              onClick={() => setIsModalOpen(true)}
            >
              New Task
            </Button>
          </div>
        }
      />

      {/* Main View State Switching */}
      {isLoading ? (
        <DashboardSkeleton />
      ) : isError ? (
        <ErrorState
          title="Unable to load dashboard data"
          message={errorMessage || 'Could not fetch tasks from backend microservice API Gateway.'}
          onRetry={fetchDashboardData}
        />
      ) : (
        <div className="space-y-8">
          {/* Welcome Banner */}
          <WelcomeSection user={user} />

          {/* Task Statistics Metric Cards */}
          <StatsGrid stats={stats} />

          {/* Quick Actions Shortcuts */}
          <QuickActions onNewTaskClick={() => setIsModalOpen(true)} />

          {/* Empty State Guard if 0 tasks */}
          {tasks.length === 0 ? (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2">
                <EmptyState
                  icon={<CheckSquare className="w-12 h-12 text-slate-500" />}
                  title="No tasks yet"
                  description="Create your first task to get started tracking your work in DevSphere."
                  actionLabel="Create Task"
                  onAction={() => setIsModalOpen(true)}
                  className="py-16"
                />
              </div>
              <div>
                <UserSummary user={user} />
              </div>
            </div>
          ) : (
            /* Multi-column Dashboard Layout */
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Left Column (2 Cols wide on Desktop) */}
              <div className="lg:col-span-2 space-y-8">
                {/* Completion Progress Overview */}
                <CompletionOverview
                  completed={stats.completed}
                  total={stats.total}
                  completionPercentage={stats.completionPercentage}
                />

                {/* Upcoming Tasks Overview */}
                <UpcomingTasks tasks={tasks} onCompleteTask={handleCompleteTask} />
              </div>

              {/* Right Column (1 Col wide on Desktop) */}
              <div className="space-y-8">
                {/* Authenticated User Summary */}
                <UserSummary user={user} />

                {/* Recent Activity Timeline */}
                <RecentActivity tasks={tasks} />
              </div>
            </div>
          )}
        </div>
      )}

      {/* New Task Creation Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create New Task"
        description="Add a task to your DevSphere workspace registry."
        footer={
          <>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleCreateTask}
              isLoading={isSubmitting}
            >
              Create Task
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreateTask} className="space-y-4">
          <Input
            label="Task Title"
            placeholder="e.g. Implement Redis Eviction Policy"
            value={taskTitle}
            onChange={(e) => setTaskTitle(e.target.value)}
            required
            autoFocus
          />

          <Select
            label="Priority Level"
            value={taskPriority}
            onChange={(e) => setTaskPriority(e.target.value as TaskPriority)}
            options={[
              { value: 'LOW', label: 'Low Priority' },
              { value: 'MEDIUM', label: 'Medium Priority' },
              { value: 'HIGH', label: 'High Priority' },
              { value: 'URGENT', label: 'Urgent' },
            ]}
          />

          <Input
            label="Due Date"
            type="date"
            value={taskDueDate}
            onChange={(e) => setTaskDueDate(e.target.value)}
          />

          <Textarea
            label="Description (Optional)"
            placeholder="Add detailed task context or microservice requirements..."
            value={taskDescription}
            onChange={(e) => setTaskDescription(e.target.value)}
            rows={3}
          />
        </form>
      </Modal>
    </div>
  );
};
