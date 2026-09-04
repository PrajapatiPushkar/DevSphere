import React from 'react';
import { cn } from '../../utils/cn';
import { AlertTriangle, CheckCircle, Info, XCircle } from 'lucide-react';

export interface AlertProps {
  type?: 'success' | 'warning' | 'error' | 'info';
  title?: string;
  children: React.ReactNode;
  className?: string;
}

export const Alert: React.FC<AlertProps> = ({ type = 'info', title, children, className }) => {
  const styles = {
    success: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300',
    warning: 'bg-amber-500/10 border-amber-500/30 text-amber-300',
    error: 'bg-rose-500/10 border-rose-500/30 text-rose-300',
    info: 'bg-sky-500/10 border-sky-500/30 text-sky-300',
  };

  const icons = {
    success: <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />,
    error: <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />,
    info: <Info className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />,
  };

  return (
    <div className={cn('p-4 rounded-xl border flex gap-3 text-sm', styles[type], className)}>
      {icons[type]}
      <div className="space-y-0.5">
        {title && <h5 className="font-semibold tracking-wide uppercase text-xs">{title}</h5>}
        <div className="text-slate-300 leading-relaxed">{children}</div>
      </div>
    </div>
  );
};
