import React from 'react';
import { PageHeader } from '../layout/PageHeader';
import { Button } from '../ui/Button';
import { Edit3, RefreshCw } from 'lucide-react';

export interface ProfileHeaderProps {
  onEditProfile: () => void;
  onRefresh: () => void;
  isRefreshing?: boolean;
}

export const ProfileHeader: React.FC<ProfileHeaderProps> = ({
  onEditProfile,
  onRefresh,
  isRefreshing = false,
}) => {
  return (
    <PageHeader
      title="Developer Profile"
      description="View and manage your platform developer identity, career statistics, and microservices account information."
      breadcrumbs={[
        { label: 'Dashboard', href: '/dashboard' },
        { label: 'Developer Profile' },
      ]}
      actions={
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={onRefresh}
            isLoading={isRefreshing}
            leftIcon={<RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />}
          >
            Refresh Data
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={onEditProfile}
            leftIcon={<Edit3 className="w-4 h-4" />}
          >
            Edit Profile
          </Button>
        </div>
      }
    />
  );
};
