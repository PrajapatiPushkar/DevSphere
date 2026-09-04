import React from 'react';
import { cn } from '../../utils/cn';
import { AlertOctagon } from 'lucide-react';
import { Button } from '../ui/Button';

export interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Something went wrong',
  message = "We couldn't load this information. Please try again or contact support if the issue persists.",
  onRetry,
  className,
}) => {
  return (
    <div className={cn('p-10 text-center flex flex-col items-center justify-center rounded-2xl border border-red-500/20 bg-red-950/10', className)}>
      <div className="p-3 bg-red-500/10 rounded-2xl mb-4 border border-red-500/20">
        <AlertOctagon className="w-8 h-8 text-red-400" />
      </div>
      <h3 className="text-base font-semibold text-slate-100 mb-1">{title}</h3>
      <p className="text-xs text-slate-400 max-w-md mb-6 leading-relaxed">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Try Again
        </Button>
      )}
    </div>
  );
};
