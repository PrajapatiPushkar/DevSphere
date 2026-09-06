import React, { forwardRef } from 'react';
import { cn } from '../../utils/cn';

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, helperText, className, required, disabled, rows = 4, id: providedId, ...props }, ref) => {
    const textareaId = providedId || (label ? `textarea-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined);
    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label htmlFor={textareaId} className="block text-xs font-semibold tracking-wide text-slate-300 uppercase">
            {label} {required && <span className="text-red-400">*</span>}
          </label>
        )}
        <textarea
          ref={ref}
          id={textareaId}
          rows={rows}
          disabled={disabled}
          aria-invalid={!!error}
          className={cn(
            'w-full bg-slate-900/90 text-slate-100 text-sm rounded-xl border px-3.5 py-2.5 transition-all duration-150 placeholder:text-slate-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/50 disabled:opacity-50 disabled:cursor-not-allowed resize-y',
            error ? 'border-red-500/80 focus:border-red-500' : 'border-slate-800 hover:border-slate-700/80 focus:border-brand-500',
            className
          )}
          {...props}
        />
        {error && <p className="text-xs text-red-400 font-medium">{error}</p>}
        {helperText && !error && <p className="text-xs text-slate-400">{helperText}</p>}
      </div>
    );
  }
);

Textarea.displayName = 'Textarea';
