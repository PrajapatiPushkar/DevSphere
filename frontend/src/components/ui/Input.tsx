import React, { forwardRef } from 'react';
import { cn } from '../../utils/cn';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, leftIcon, rightIcon, className, required, disabled, id: providedId, ...props }, ref) => {
    const inputId = providedId || (label ? `input-${label.toLowerCase().replace(/\s+/g, '-')}` : undefined);
    const helperId = inputId ? `${inputId}-helper` : undefined;

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label htmlFor={inputId} className="block text-xs font-semibold tracking-wide text-slate-300 uppercase">
            {label} {required && <span className="text-red-400">*</span>}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <div className="absolute left-3 text-slate-400 pointer-events-none shrink-0">
              {leftIcon}
            </div>
          )}
          <input
            ref={ref}
            id={inputId}
            disabled={disabled}
            aria-invalid={!!error}
            aria-describedby={helperId}
            className={cn(
              'w-full bg-slate-900/90 text-slate-100 text-sm rounded-xl border px-3.5 py-2.5 transition-all duration-150 placeholder:text-slate-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/50 disabled:opacity-50 disabled:cursor-not-allowed min-h-[42px]',
              error ? 'border-red-500/80 focus:border-red-500' : 'border-slate-800 hover:border-slate-700/80 focus:border-brand-500',
              leftIcon && 'pl-10',
              rightIcon && 'pr-10',
              className
            )}
            {...props}
          />
          {rightIcon && (
            <div className="absolute right-3 text-slate-400 shrink-0">
              {rightIcon}
            </div>
          )}
        </div>
        {error && (
          <p id={helperId} className="text-xs text-red-400 font-medium">
            {error}
          </p>
        )}
        {helperText && !error && (
          <p id={helperId} className="text-xs text-slate-400">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
