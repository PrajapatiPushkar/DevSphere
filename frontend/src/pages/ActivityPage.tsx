import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { activityService } from '../services/activityService';
import { ActivityItemData } from '../types/notificationTypes';
import { PageHeader } from '../components/layout/PageHeader';
import { ActivityTimeline } from '../components/activity/ActivityTimeline';
import { Button } from '../components/ui/Button';
import { ErrorState } from '../components/common/ErrorState';
import { RefreshCw, Activity as ActivityIcon } from 'lucide-react';

export const ActivityPage: React.FC = () => {
  const { user } = useAuth();
  const [activities, setActivities] = useState<ActivityItemData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchTimeline = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await activityService.getActivityTimeline();
      setActivities(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load activity timeline');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTimeline();
  }, [fetchTimeline]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchTimeline();
    setIsRefreshing(false);
  };

  if (!user) {
    return (
      <div className="py-12">
        <ErrorState title="Authentication Required" message="Please log in to view the activity timeline." />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12 animate-in fade-in duration-200">
      <PageHeader
        title="Activity Timeline"
        description="Chronological event log generated from microservices task management and developer profile telemetry."
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Activity Timeline' },
        ]}
        actions={
          <Button
            variant="secondary"
            size="sm"
            onClick={handleRefresh}
            isLoading={isRefreshing}
            leftIcon={<RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />}
          >
            Refresh Feed
          </Button>
        }
      />

      <ActivityTimeline
        activities={activities}
        isLoading={isLoading}
        error={error}
        onRetry={fetchTimeline}
      />
    </div>
  );
};
