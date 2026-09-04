import React, { forwardRef } from 'react';
import { cn } from '../../utils/cn';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: SelectOption[];
  error?: string;
  helperText?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, helperText, className, required, disabled, ...props }, ref) => {
    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label className="block text-xs font-semibold tracking-wide text-slate-300 uppercase">
            {label} {required && <span className="text-red-400">*</span>}
          </label>
        )}
        <select
          ref={ref}
          disabled={disabled}
          className={cn(
            'w-full bg-slate-900/90 text-slate-100 text-sm rounded-xl border px-3.5 py-2.5 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-brand-500/50 disabled:opacity-50 disabled:cursor-not-allowed appearance-none cursor-pointer',
            error ? 'border-red-500/80 focus:border-red-500' : 'border-slate-800 hover:border-slate-700 focus:border-brand-500',
            className
          )}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value} className="bg-slate-900 text-slate-100">
              {opt.label}
            </option>
          ))}
        </select>
        {error && <p className="text-xs text-red-400 font-medium">{error}</p>}
        {helperText && !error && <p className="text-xs text-slate-400">{helperText}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';
