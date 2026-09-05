import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { PageHeader } from '../components/layout/PageHeader';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/common/EmptyState';
import { ErrorState } from '../components/common/ErrorState';
import { useToast } from '../context/ToastContext';
import { taskService } from '../services/taskService';
import { Task, TaskStatus, TaskPriority, CreateTaskInput, UpdateTaskInput } from '../types';
import { TaskFilters, TaskFilterState } from '../components/tasks/TaskFilters';
import { TaskTable } from '../components/tasks/TaskTable';
import { TaskFormModal } from '../components/tasks/TaskFormModal';
import { DeleteTaskModal } from '../components/tasks/DeleteTaskModal';
import { TaskListSkeleton } from '../components/tasks/TaskListSkeleton';
import { Plus, RefreshCw, CheckSquare, ChevronLeft, ChevronRight, FilterX } from 'lucide-react';

export const TasksPage: React.FC = () => {
  const { showToast } = useToast();

  const [rawTasks, setRawTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isError, setIsError] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');

  // Pagination state
  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(20);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Filter & Search state
  const [filters, setFilters] = useState<TaskFilterState>({
    search: '',
    status: 'ALL',
    priority: 'ALL',
    dueDateFilter: 'ALL',
    sort: 'createdAt,desc',
  });

  // Modal states
  const [isFormModalOpen, setIsFormModalOpen] = useState<boolean>(false);
  const [taskToEdit, setTaskToEdit] = useState<Task | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState<boolean>(false);
  const [taskToDelete, setTaskToDelete] = useState<Task | null>(null);

  const fetchTasks = useCallback(async () => {
    setIsLoading(true);
    setIsError(false);
    setErrorMessage('');

    try {
      const queryStatus = filters.status !== 'ALL' ? (filters.status as TaskStatus) : undefined;
      const queryPriority = filters.priority !== 'ALL' ? (filters.priority as TaskPriority) : undefined;

      const pageResponse = await taskService.getTasks({
        status: queryStatus,
        priority: queryPriority,
        page,
        size: pageSize,
        sort: filters.sort,
      });

      setRawTasks(pageResponse.content || []);
      setTotalPages(pageResponse.totalPages || 1);
      setTotalElements(pageResponse.totalElements || 0);
    } catch (err: any) {
      setIsError(true);
      setErrorMessage(err?.message || 'Failed to fetch task list from backend user-service.');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, filters.status, filters.priority, filters.sort]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const handleFilterChange = (key: keyof TaskFilterState, value: string) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
    setPage(0); // Reset to first page when filter changes
  };

  const handleClearFilters = () => {
    setFilters({
      search: '',
      status: 'ALL',
      priority: 'ALL',
      dueDateFilter: 'ALL',
      sort: 'createdAt,desc',
    });
    setPage(0);
  };

  // Client-side search and due date filtering on fetched records
  const filteredTasks = useMemo(() => {
    return rawTasks.filter((t) => {
      // Keyword Search Filter
      if (filters.search.trim()) {
        const query = filters.search.toLowerCase();
        const titleMatch = t.title.toLowerCase().includes(query);
        const descMatch = t.description?.toLowerCase().includes(query) || false;
        if (!titleMatch && !descMatch) return false;
      }

      // Due Date Filter
      if (filters.dueDateFilter !== 'ALL') {
        const now = new Date();
        const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        const endOfDay = startOfDay + 86400000;

        if (filters.dueDateFilter === 'OVERDUE') {
          const isOverdue = t.overdue || (t.dueDate && new Date(t.dueDate).getTime() < Date.now() && t.status !== 'COMPLETED' && t.status !== 'CANCELLED');
          if (!isOverdue) return false;
        } else if (filters.dueDateFilter === 'DUE_TODAY') {
          if (!t.dueDate) return false;
          const dueTime = new Date(t.dueDate).getTime();
          if (dueTime < startOfDay || dueTime >= endOfDay) return false;
        } else if (filters.dueDateFilter === 'UPCOMING') {
          if (!t.dueDate) return false;
          const dueTime = new Date(t.dueDate).getTime();
          if (dueTime < Date.now()) return false;
        }
      }

      return true;
    });
  }, [rawTasks, filters.search, filters.dueDateFilter]);

  // Modal Triggers
  const openCreateModal = () => {
    setTaskToEdit(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (task: Task) => {
    setTaskToEdit(task);
    setIsFormModalOpen(true);
  };

  const openDeleteModal = (task: Task) => {
    setTaskToDelete(task);
    setIsDeleteModalOpen(true);
  };

  // API Mutators
  const handleCreateSubmit = async (input: CreateTaskInput) => {
    await taskService.createTask(input);
    showToast('Task created successfully', 'success', 'Task Saved');
    fetchTasks();
  };

  const handleEditSubmit = async (taskId: number, input: UpdateTaskInput) => {
    await taskService.updateTask(taskId, input);
    showToast(`Task-${taskId} updated successfully`, 'success', 'Task Saved');
    fetchTasks();
  };

  const handleCompleteTask = async (taskId: number) => {
    try {
      await taskService.completeTask(taskId);
      showToast(`Task-${taskId} marked as completed`, 'success');
      fetchTasks();
    } catch (err: any) {
      showToast(err?.message || 'Failed to complete task', 'error');
    }
  };

  const handleStartTask = async (taskId: number) => {
    try {
      await taskService.startTask(taskId);
      showToast(`Task-${taskId} marked as in progress`, 'info');
      fetchTasks();
    } catch (err: any) {
      showToast(err?.message || 'Failed to start task', 'error');
    }
  };

  const handleReopenTask = async (taskId: number) => {
    try {
      await taskService.reopenTask(taskId);
      showToast(`Task-${taskId} reopened successfully`, 'info');
      fetchTasks();
    } catch (err: any) {
      showToast(err?.message || 'Failed to reopen task', 'error');
    }
  };

  const handleCancelTask = async (taskId: number) => {
    try {
      await taskService.cancelTask(taskId);
      showToast(`Task-${taskId} cancelled`, 'warning');
      fetchTasks();
    } catch (err: any) {
      showToast(err?.message || 'Failed to cancel task', 'error');
    }
  };

  const handleDeleteConfirm = async (taskId: number) => {
    try {
      await taskService.deleteTask(taskId);
      showToast(`Task-${taskId} deleted successfully`, 'success');
      fetchTasks();
    } catch (err: any) {
      showToast(err?.message || 'Failed to delete task', 'error');
    }
  };

  const hasActiveFilters =
    filters.search.trim() !== '' ||
    filters.status !== 'ALL' ||
    filters.priority !== 'ALL' ||
    filters.dueDateFilter !== 'ALL' ||
    filters.sort !== 'createdAt,desc';

  return (
    <div className="space-y-6 pb-12">
      {/* Page Header */}
      <PageHeader
        title="Task Management"
        description="Organize, filter, track, and manage microservice tasks across your DevSphere platform."
        breadcrumbs={[{ label: 'Console', href: '/dashboard' }, { label: 'Tasks' }]}
        actions={
          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="sm"
              leftIcon={<RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />}
              onClick={fetchTasks}
              disabled={isLoading}
            >
              Refresh
            </Button>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Plus className="w-4 h-4" />}
              onClick={openCreateModal}
            >
              New Task
            </Button>
          </div>
        }
      />

      {/* Filter Controls Toolbar */}
      <TaskFilters
        filters={filters}
        onFilterChange={handleFilterChange}
        onClearFilters={handleClearFilters}
        totalResults={filteredTasks.length}
      />

      {/* Main Content Area: Loading / Error / Empty / Table */}
      {isLoading ? (
        <TaskListSkeleton />
      ) : isError ? (
        <ErrorState
          title="Unable to load tasks"
          message={errorMessage || 'Could not reach backend microservices.'}
          onRetry={fetchTasks}
        />
      ) : rawTasks.length === 0 && !hasActiveFilters ? (
        <EmptyState
          icon={<CheckSquare className="w-12 h-12 text-slate-500" />}
          title="No tasks registered yet"
          description="Create your first task to start managing microservice development and deployment activities."
          actionLabel="Create First Task"
          onAction={openCreateModal}
          className="py-16"
        />
      ) : filteredTasks.length === 0 ? (
        <EmptyState
          icon={<FilterX className="w-12 h-12 text-slate-500" />}
          title="No matching tasks found"
          description="No tasks match your search query or filter criteria. Try adjusting your filters."
          actionLabel="Clear All Filters"
          onAction={handleClearFilters}
          className="py-16"
        />
      ) : (
        <div className="space-y-4">
          <TaskTable
            tasks={filteredTasks}
            onEdit={openEditModal}
            onComplete={handleCompleteTask}
            onStart={handleStartTask}
            onReopen={handleReopenTask}
            onCancel={handleCancelTask}
            onDelete={openDeleteModal}
          />

          {/* Pagination Navigation Bar */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400">
            <div>
              Showing <span className="font-bold text-slate-200">{filteredTasks.length}</span> of{' '}
              <span className="font-bold text-slate-200">{totalElements}</span> tasks (Page{' '}
              <span className="font-mono text-slate-200">{page + 1}</span> of{' '}
              <span className="font-mono text-slate-200">{totalPages}</span>)
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<ChevronLeft className="w-4 h-4" />}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                rightIcon={<ChevronRight className="w-4 h-4" />}
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages || isLoading}
              >
                Next
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Create & Edit Task Modal */}
      <TaskFormModal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        taskToEdit={taskToEdit}
        onSubmitCreate={handleCreateSubmit}
        onSubmitEdit={handleEditSubmit}
      />

      {/* Delete Task Confirmation Modal */}
      <DeleteTaskModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        task={taskToDelete}
        onConfirmDelete={handleDeleteConfirm}
      />
    </div>
  );
};
