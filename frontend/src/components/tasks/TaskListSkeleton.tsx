import React from 'react';
import { Skeleton } from '../common/Skeleton';
import { Card } from '../ui/Card';

export const TaskListSkeleton: React.FC = () => {
  return (
    <div className="space-y-4 animate-pulse" data-testid="task-list-skeleton">
      {/* Table header skeleton */}
      <Card glass className="p-4 flex items-center justify-between border-slate-800">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-4 w-20" />
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-28" />
        <Skeleton className="h-4 w-12" />
      </Card>

      {/* Row skeletons */}
      {Array.from({ length: 5 }).map((_, idx) => (
        <Card key={idx} glass className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-slate-800/80">
          <div className="space-y-2 flex-1">
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="h-3 w-1/3" />
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <Skeleton className="h-6 w-16 rounded-lg" />
            <Skeleton className="h-6 w-20 rounded-lg" />
            <Skeleton className="h-6 w-24 rounded-lg" />
            <Skeleton className="h-8 w-8 rounded-lg" />
          </div>
        </Card>
      ))}
    </div>
  );
};
