import React from 'react';
import { User, Shield, Sliders, Palette } from 'lucide-react';
import { cn } from '../../utils/cn';

export type SettingsTab = 'account' | 'security' | 'preferences' | 'appearance';

export interface SettingsLayoutProps {
  activeTab: SettingsTab;
  onTabChange: (tab: SettingsTab) => void;
  children: React.ReactNode;
}

export const SettingsLayout: React.FC<SettingsLayoutProps> = ({
  activeTab,
  onTabChange,
  children,
}) => {
  const tabs = [
    { id: 'account' as SettingsTab, label: 'Account Information', icon: <User className="w-4 h-4" /> },
    { id: 'security' as SettingsTab, label: 'Password & Security', icon: <Shield className="w-4 h-4" /> },
    { id: 'preferences' as SettingsTab, label: 'User Preferences', icon: <Sliders className="w-4 h-4" /> },
    { id: 'appearance' as SettingsTab, label: 'Appearance & Theme', icon: <Palette className="w-4 h-4" /> },
  ];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 items-start">
      {/* Desktop Sidebar & Mobile Horizontal Tabs */}
      <nav className="lg:col-span-1 flex flex-row lg:flex-col overflow-x-auto gap-1 border-b lg:border-b-0 lg:border-r border-slate-800 pb-4 lg:pb-0 lg:pr-4">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-semibold whitespace-nowrap transition-all duration-150 text-left shrink-0 w-auto lg:w-full',
                isActive
                  ? 'bg-brand-600/15 text-brand-400 border border-brand-500/30 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 border border-transparent'
              )}
            >
              {tab.icon}
              <span>{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Main Settings Content Area */}
      <div className="lg:col-span-3 space-y-6">{children}</div>
    </div>
  );
};
