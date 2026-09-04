import React, { forwardRef } from 'react';
import { cn } from '../../utils/cn';

export interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string;
  description?: string;
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, description, className, disabled, ...props }, ref) => {
    return (
      <label className="relative flex items-start gap-3 cursor-pointer select-none">
        <input
          type="checkbox"
          ref={ref}
          disabled={disabled}
          className={cn(
            'w-4 h-4 mt-0.5 rounded border-slate-700 bg-slate-900 text-brand-600 focus:ring-brand-500 focus:ring-offset-slate-950 transition duration-150 disabled:opacity-50 cursor-pointer',
            className
          )}
          {...props}
        />
        {(label || description) && (
          <div className="text-sm">
            {label && <span className="font-medium text-slate-200">{label}</span>}
            {description && <p className="text-xs text-slate-400">{description}</p>}
          </div>
        )}
      </label>
    );
  }
);

Checkbox.displayName = 'Checkbox';
