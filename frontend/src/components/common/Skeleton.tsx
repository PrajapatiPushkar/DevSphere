import React from 'react';
import { cn } from '../../utils/cn';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {}

export const Skeleton: React.FC<SkeletonProps> = ({ className, ...props }) => {
  return (
    <div
      className={cn('animate-pulse rounded-lg bg-slate-800/80', className)}
      {...props}
    />
  );
};

export const CardSkeleton: React.FC = () => (
  <div className="p-6 rounded-2xl border border-slate-800 bg-slate-900/90 space-y-4">
    <Skeleton className="h-5 w-1/3" />
    <Skeleton className="h-4 w-3/4" />
    <Skeleton className="h-20 w-full" />
    <div className="flex justify-between items-center pt-2">
      <Skeleton className="h-8 w-24" />
      <Skeleton className="h-8 w-16" />
    </div>
  </div>
);

export const TableSkeleton: React.FC = () => (
  <div className="border border-slate-800 rounded-2xl bg-slate-900/80 p-4 space-y-3">
    <Skeleton className="h-10 w-full" />
    {Array.from({ length: 4 }).map((_, i) => (
      <Skeleton key={i} className="h-12 w-full" />
    ))}
  </div>
);
