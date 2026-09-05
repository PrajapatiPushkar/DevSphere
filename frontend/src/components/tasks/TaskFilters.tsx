import React from 'react';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Button } from '../ui/Button';
import { Search, X, Filter, ArrowUpDown } from 'lucide-react';
import { TaskStatus, TaskPriority } from '../../types';

export interface TaskFilterState {
  search: string;
  status: string;
  priority: string;
  dueDateFilter: string;
  sort: string;
}

export interface TaskFiltersProps {
  filters: TaskFilterState;
  onFilterChange: (key: keyof TaskFilterState, value: string) => void;
  onClearFilters: () => void;
  totalResults?: number;
}

export const TaskFilters: React.FC<TaskFiltersProps> = ({
  filters,
  onFilterChange,
  onClearFilters,
  totalResults,
}) => {
  const hasActiveFilters =
    filters.search.trim() !== '' ||
    filters.status !== 'ALL' ||
    filters.priority !== 'ALL' ||
    filters.dueDateFilter !== 'ALL' ||
    filters.sort !== 'createdAt,desc';

  return (
    <div className="p-4 sm:p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4 shadow-md">
      {/* Top Row: Search & Action Buttons */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="w-full sm:w-80">
          <Input
            placeholder="Search tasks by title or description..."
            value={filters.search}
            onChange={(e) => onFilterChange('search', e.target.value)}
            leftIcon={<Search className="w-4 h-4" />}
            rightIcon={
              filters.search ? (
                <button
                  type="button"
                  onClick={() => onFilterChange('search', '')}
                  className="hover:text-slate-200 p-0.5 rounded"
                  title="Clear search"
                >
                  <X className="w-4 h-4" />
                </button>
              ) : undefined
            }
          />
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto w-full sm:w-auto justify-between sm:justify-end">
          {totalResults !== undefined && (
            <span className="text-xs font-mono text-slate-400">
              {totalResults} {totalResults === 1 ? 'task' : 'tasks'} found
            </span>
          )}

          {hasActiveFilters && (
            <Button
              variant="ghost"
              size="sm"
              leftIcon={<X className="w-3.5 h-3.5" />}
              onClick={onClearFilters}
              className="text-slate-400 hover:text-slate-200"
            >
              Clear Filters
            </Button>
          )}
        </div>
      </div>

      {/* Bottom Row: Filter Dropdowns */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1 border-t border-slate-800/80">
        <Select
          label="Status"
          value={filters.status}
          onChange={(e) => onFilterChange('status', e.target.value)}
          options={[
            { value: 'ALL', label: 'All Statuses' },
            { value: 'TODO', label: 'To-Do' },
            { value: 'IN_PROGRESS', label: 'In Progress' },
            { value: 'COMPLETED', label: 'Completed' },
            { value: 'CANCELLED', label: 'Cancelled' },
            { value: 'ARCHIVED', label: 'Archived' },
          ]}
        />

        <Select
          label="Priority"
          value={filters.priority}
          onChange={(e) => onFilterChange('priority', e.target.value)}
          options={[
            { value: 'ALL', label: 'All Priorities' },
            { value: 'URGENT', label: 'Urgent' },
            { value: 'HIGH', label: 'High Priority' },
            { value: 'MEDIUM', label: 'Medium Priority' },
            { value: 'LOW', label: 'Low Priority' },
          ]}
        />

        <Select
          label="Due Date"
          value={filters.dueDateFilter}
          onChange={(e) => onFilterChange('dueDateFilter', e.target.value)}
          options={[
            { value: 'ALL', label: 'All Dates' },
            { value: 'OVERDUE', label: 'Overdue' },
            { value: 'DUE_TODAY', label: 'Due Today' },
            { value: 'UPCOMING', label: 'Upcoming' },
          ]}
        />

        <Select
          label="Sort By"
          value={filters.sort}
          onChange={(e) => onFilterChange('sort', e.target.value)}
          options={[
            { value: 'createdAt,desc', label: 'Newest First' },
            { value: 'createdAt,asc', label: 'Oldest First' },
            { value: 'dueDate,asc', label: 'Due Date (Earliest)' },
            { value: 'dueDate,desc', label: 'Due Date (Latest)' },
            { value: 'priority,desc', label: 'Priority (High to Low)' },
            { value: 'title,asc', label: 'Title (A-Z)' },
          ]}
        />
      </div>
    </div>
  );
};
