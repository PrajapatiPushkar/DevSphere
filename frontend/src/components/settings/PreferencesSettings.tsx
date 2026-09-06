import React, { useState, useEffect } from 'react';
import { UserPreferences } from '../../types';
import { useToast } from '../../context/ToastContext';
import { Card } from '../ui/Card';
import { Select } from '../ui/Select';
import { Checkbox } from '../ui/Checkbox';
import { Button } from '../ui/Button';
import { Sliders, Bell, LayoutList } from 'lucide-react';

export const PreferencesSettings: React.FC = () => {
  const { showToast } = useToast();

  const [preferences, setPreferences] = useState<UserPreferences>(() => {
    const saved = localStorage.getItem('devsphere_user_preferences');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        // Fallback default
      }
    }
    return {
      theme: 'dark',
      compactView: false,
      emailNotifications: true,
      defaultPageSize: 50,
    };
  });

  const [isSaving, setIsSaving] = useState(false);

  const handleSave = () => {
    setIsSaving(true);
    try {
      localStorage.setItem('devsphere_user_preferences', JSON.stringify(preferences));
      showToast('Preferences saved successfully', 'success');
    } catch {
      showToast('Failed to save preferences', 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Card className="p-6 space-y-6">
      <div className="border-b border-slate-800 pb-4">
        <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
          <Sliders className="w-4 h-4 text-brand-400" />
          Application Preferences
        </h3>
        <p className="text-xs text-slate-400 mt-1">
          Customize your DevSphere UI behavior, pagination defaults, and telemetry notifications.
        </p>
      </div>

      <div className="space-y-5">
        <div className="space-y-2">
          <Select
            label="Default Task Page Size"
            value={preferences.defaultPageSize.toString()}
            onChange={(e) =>
              setPreferences({
                ...preferences,
                defaultPageSize: Number(e.target.value),
              })
            }
            options={[
              { value: '10', label: '10 Items per page' },
              { value: '25', label: '25 Items per page' },
              { value: '50', label: '50 Items per page (Default)' },
              { value: '100', label: '100 Items per page' },
            ]}
          />
          <p className="text-xs text-slate-400">
            Controls task list pagination size across Task Management and Dashboard screens.
          </p>
        </div>

        <div className="pt-3 border-t border-slate-800 space-y-4">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-brand-400 mt-0.5">
              <LayoutList className="w-4 h-4" />
            </div>
            <div className="flex-1 space-y-1">
              <Checkbox
                label="Enable Compact View Mode"
                checked={preferences.compactView}
                onChange={(e) =>
                  setPreferences({
                    ...preferences,
                    compactView: e.target.checked,
                  })
                }
              />
              <p className="text-xs text-slate-400 pl-6">
                Display dense data tables and reduced card padding for power developer workflows.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-3">
            <div className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-emerald-400 mt-0.5">
              <Bell className="w-4 h-4" />
            </div>
            <div className="flex-1 space-y-1">
              <Checkbox
                label="Task Overdue Telemetry Alerts"
                checked={preferences.emailNotifications}
                onChange={(e) =>
                  setPreferences({
                    ...preferences,
                    emailNotifications: e.target.checked,
                  })
                }
              />
              <p className="text-xs text-slate-400 pl-6">
                Receive browser toasts when microservice tasks exceed target due dates.
              </p>
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <Button variant="primary" size="sm" onClick={handleSave} isLoading={isSaving}>
            Save Preferences
          </Button>
        </div>
      </div>
    </Card>
  );
};
