import React from 'react';
import { Skeleton } from '../common/Skeleton';
import { Card } from '../ui/Card';

export const DashboardSkeleton: React.FC = () => {
  return (
    <div className="space-y-8 animate-pulse" data-testid="dashboard-skeleton">
      {/* Welcome Banner Skeleton */}
      <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <Skeleton className="h-4 w-48" />
        <Skeleton className="h-8 w-72" />
        <Skeleton className="h-4 w-96" />
      </div>

      {/* Stats Grid Skeleton */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {Array.from({ length: 4 }).map((_, idx) => (
          <Card key={idx} glass className="p-5 space-y-3">
            <div className="flex justify-between items-center">
              <Skeleton className="h-3 w-24" />
              <Skeleton className="h-8 w-8 rounded-xl" />
            </div>
            <Skeleton className="h-8 w-16" />
            <Skeleton className="h-3 w-32" />
          </Card>
        ))}
      </div>

      {/* Quick Actions Skeleton */}
      <Card glass className="p-6 space-y-4">
        <Skeleton className="h-5 w-40" />
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <Skeleton className="h-10 w-full rounded-xl" />
          <Skeleton className="h-10 w-full rounded-xl" />
          <Skeleton className="h-10 w-full rounded-xl" />
        </div>
      </Card>

      {/* Main Content Layout Grid Skeleton */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          {/* Completion Overview Skeleton */}
          <Card glass className="p-6 space-y-4">
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-4 w-full rounded-full" />
            <Skeleton className="h-4 w-2/3" />
          </Card>

          {/* Upcoming Tasks Skeleton */}
          <Card glass className="p-6 space-y-4">
            <Skeleton className="h-5 w-40" />
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-16 w-full rounded-xl" />
            ))}
          </Card>
        </div>

        <div className="space-y-6">
          {/* User Summary Skeleton */}
          <Card glass className="p-6 space-y-4">
            <Skeleton className="h-5 w-36" />
            <Skeleton className="h-20 w-full rounded-xl" />
          </Card>

          {/* Recent Activity Skeleton */}
          <Card glass className="p-6 space-y-4">
            <Skeleton className="h-5 w-36" />
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full rounded-xl" />
            ))}
          </Card>
        </div>
      </div>
    </div>
  );
};
