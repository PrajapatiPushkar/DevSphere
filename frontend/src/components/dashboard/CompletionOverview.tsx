import React from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../ui/Card';
import { CheckCircle2, Target } from 'lucide-react';
import { Badge } from '../ui/Badge';

export interface CompletionOverviewProps {
  completed: number;
  total: number;
  completionPercentage: number;
}

export const CompletionOverview: React.FC<CompletionOverviewProps> = ({
  completed,
  total,
  completionPercentage,
}) => {
  const safePercentage = Math.min(100, Math.max(0, isNaN(completionPercentage) ? 0 : completionPercentage));

  return (
    <Card glass className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <Target className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100 tracking-tight">Task Completion</h3>
            <p className="text-xs text-slate-400">Overall goal execution progress</p>
          </div>
        </div>
        <Badge variant={safePercentage >= 80 ? 'success' : safePercentage >= 40 ? 'warning' : 'neutral'}>
          {safePercentage}% Complete
        </Badge>
      </div>

      {/* Progress Bar Container */}
      <div className="space-y-2">
        <div className="flex justify-between items-center text-xs font-semibold">
          <span className="text-slate-300">Overall Progress</span>
          <span className="text-emerald-400 font-mono font-bold">{safePercentage}%</span>
        </div>

        <div className="h-3 w-full bg-slate-900 rounded-full overflow-hidden border border-slate-800 p-0.5">
          <div
            className="h-full bg-gradient-to-r from-brand-500 via-indigo-500 to-emerald-400 rounded-full transition-all duration-500 ease-out shadow-glow"
            style={{ width: `${safePercentage}%` }}
          />
        </div>

        <div className="flex items-center justify-between text-xs text-slate-400 pt-1">
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>{completed} of {total} tasks completed</span>
          </div>
          <span>{total - completed} remaining</span>
        </div>
      </div>
    </Card>
  );
};
