import React from 'react';
import { useToast } from '../../context/ToastContext';
import { cn } from '../../utils/cn';
import { CheckCircle, AlertTriangle, XCircle, Info, X } from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useToast();

  if (toasts.length === 0) return null;

  const icons = {
    success: <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />,
    error: <XCircle className="w-5 h-5 text-rose-400 shrink-0" />,
    info: <Info className="w-5 h-5 text-sky-400 shrink-0" />,
  };

  const borderColors = {
    success: 'border-emerald-500/40',
    warning: 'border-amber-500/40',
    error: 'border-rose-500/40',
    info: 'border-sky-500/40',
  };

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none px-4">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={cn(
            'pointer-events-auto p-4 rounded-xl bg-slate-900 border shadow-2xl flex items-start gap-3 animate-in slide-in-from-bottom-5 duration-200 glass-card',
            borderColors[toast.type]
          )}
        >
          {icons[toast.type]}
          <div className="flex-1 min-w-0">
            {toast.title && <h5 className="text-xs font-semibold text-slate-100 uppercase tracking-wide">{toast.title}</h5>}
            <p className="text-xs text-slate-300 leading-snug mt-0.5">{toast.message}</p>
          </div>
          <button
            onClick={() => removeToast(toast.id)}
            className="text-slate-400 hover:text-slate-200 p-1 rounded hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
    </div>
  );
};
