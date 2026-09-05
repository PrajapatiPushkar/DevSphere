import React from 'react';
import { Card } from '../ui/Card';
import { cn } from '../../utils/cn';

export interface StatCardProps {
  title: string;
  value: number | string;
  subtitle?: string;
  icon: React.ReactNode;
  variant?: 'brand' | 'warning' | 'success' | 'danger';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  variant = 'brand',
}) => {
  const variantStyles = {
    brand: 'text-brand-400 bg-brand-500/10 border-brand-500/20',
    warning: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
    success: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
    danger: 'text-rose-400 bg-rose-500/10 border-rose-500/20',
  };

  return (
    <Card glass className="p-5 relative overflow-hidden group hover:border-slate-700 transition-all duration-200">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{title}</span>
        <div className={cn('p-2.5 rounded-xl border transition-colors', variantStyles[variant])}>
          {icon}
        </div>
      </div>

      <div className="mt-3 space-y-1">
        <div className="text-3xl font-black text-slate-100 tracking-tight font-mono">{value}</div>
        {subtitle && (
          <p className="text-xs text-slate-400 font-medium">{subtitle}</p>
        )}
      </div>
    </Card>
  );
};
