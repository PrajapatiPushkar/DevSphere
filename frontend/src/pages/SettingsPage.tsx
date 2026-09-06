import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { PageHeader } from '../components/layout/PageHeader';
import { SettingsLayout, SettingsTab } from '../components/settings/SettingsLayout';
import { AccountSettings } from '../components/settings/AccountSettings';
import { SecuritySettings } from '../components/settings/SecuritySettings';
import { PreferencesSettings } from '../components/settings/PreferencesSettings';
import { AppearanceSettings } from '../components/settings/AppearanceSettings';
import { ErrorState } from '../components/common/ErrorState';

export const SettingsPage: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<SettingsTab>('account');

  if (!user) {
    return (
      <div className="py-12">
        <ErrorState
          title="Authentication Required"
          message="Please log in to access account settings."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12 animate-in fade-in duration-200">
      <PageHeader
        title="Settings & Governance"
        description="Manage your account preferences, security telemetry settings, and application appearance."
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Settings' },
        ]}
      />

      <SettingsLayout activeTab={activeTab} onTabChange={setActiveTab}>
        {activeTab === 'account' && <AccountSettings user={user} />}
        {activeTab === 'security' && <SecuritySettings />}
        {activeTab === 'preferences' && <PreferencesSettings />}
        {activeTab === 'appearance' && <AppearanceSettings />}
      </SettingsLayout>
    </div>
  );
};
