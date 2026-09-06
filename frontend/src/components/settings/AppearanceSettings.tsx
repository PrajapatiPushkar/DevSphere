import React, { useState, useEffect } from 'react';
import { useToast } from '../../context/ToastContext';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Palette, Moon, Sun, Monitor, Check } from 'lucide-react';
import { cn } from '../../utils/cn';

export type ThemeMode = 'dark' | 'light' | 'system';

export const AppearanceSettings: React.FC = () => {
  const { showToast } = useToast();

  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    return (localStorage.getItem('devsphere_theme') as ThemeMode) || 'dark';
  });

  const applyTheme = (mode: ThemeMode) => {
    const root = document.documentElement;
    if (mode === 'dark') {
      root.classList.add('dark');
    } else if (mode === 'light') {
      root.classList.remove('dark');
    } else if (mode === 'system') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (prefersDark) {
        root.classList.add('dark');
      } else {
        root.classList.remove('dark');
      }
    }
  };

  useEffect(() => {
    applyTheme(themeMode);
  }, [themeMode]);

  const handleSelectTheme = (mode: ThemeMode) => {
    setThemeMode(mode);
    localStorage.setItem('devsphere_theme', mode);
    applyTheme(mode);
    showToast(`Theme updated to ${mode.toUpperCase()} mode`, 'info');
  };

  const themes = [
    {
      id: 'dark' as ThemeMode,
      title: 'Dark Theme (Standard)',
      description: 'Sleek slate-950 backdrop with indigo brand glows designed for developer consoles.',
      icon: <Moon className="w-5 h-5 text-indigo-400" />,
      tag: 'Default',
    },
    {
      id: 'light' as ThemeMode,
      title: 'Light Theme',
      description: 'Clean high-contrast theme for bright environments.',
      icon: <Sun className="w-5 h-5 text-amber-400" />,
      tag: 'Supported',
    },
    {
      id: 'system' as ThemeMode,
      title: 'System Default',
      description: 'Automatically match OS display settings preferences.',
      icon: <Monitor className="w-5 h-5 text-emerald-400" />,
      tag: 'Auto',
    },
  ];

  return (
    <Card className="p-6 space-y-6">
      <div className="border-b border-slate-800 pb-4">
        <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
          <Palette className="w-4 h-4 text-brand-400" />
          Appearance & Visual Styling
        </h3>
        <p className="text-xs text-slate-400 mt-1">
          Select your preferred theme display mode for DevSphere dashboard interfaces.
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {themes.map((theme) => {
          const isSelected = themeMode === theme.id;
          return (
            <div
              key={theme.id}
              onClick={() => handleSelectTheme(theme.id)}
              className={cn(
                'cursor-pointer p-4 rounded-xl border transition-all duration-200 space-y-3 flex flex-col justify-between relative group',
                isSelected
                  ? 'bg-brand-600/15 border-brand-500 shadow-glow'
                  : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
              )}
            >
              <div className="flex items-center justify-between">
                <div className="p-2 rounded-lg bg-slate-900 border border-slate-800">{theme.icon}</div>
                {isSelected ? (
                  <span className="p-1 rounded-full bg-brand-500 text-white">
                    <Check className="w-3.5 h-3.5" />
                  </span>
                ) : (
                  <Badge variant="neutral" size="sm">
                    {theme.tag}
                  </Badge>
                )}
              </div>

              <div>
                <h4 className="text-sm font-semibold text-slate-100">{theme.title}</h4>
                <p className="text-xs text-slate-400 mt-1">{theme.description}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="pt-4 border-t border-slate-800 text-xs text-slate-400 font-mono">
        Active theme engine: <span className="text-brand-300 font-bold">{themeMode.toUpperCase()}</span> (Persisted in localStorage)
      </div>
    </Card>
  );
};
