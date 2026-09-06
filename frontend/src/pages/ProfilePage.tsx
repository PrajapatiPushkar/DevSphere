import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';
import { taskService } from '../services/taskService';
import { User } from '../types';
import { ProfileHeader } from '../components/profile/ProfileHeader';
import { ProfileAvatar } from '../components/profile/ProfileAvatar';
import { ProfileStats, ProfileStatsData } from '../components/profile/ProfileStats';
import { ProfileInformation } from '../components/profile/ProfileInformation';
import { EditProfileModal } from '../components/profile/EditProfileModal';
import { ErrorState } from '../components/common/ErrorState';
import { Skeleton } from '../components/common/Skeleton';
import { Card } from '../components/ui/Card';

export const ProfilePage: React.FC = () => {
  const { user: authUser, updateUser } = useAuth();

  const [profile, setProfile] = useState<User | null>(authUser);
  const [stats, setStats] = useState<ProfileStatsData | null>(null);
  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const fetchProfileAndStats = useCallback(async () => {
    setError(null);
    setIsLoadingProfile(true);
    setIsLoadingStats(true);

    try {
      // Fetch user profile from backend
      const userProfile = await userService.getMyProfile();
      setProfile(userProfile);
      updateUser(userProfile);
    } catch (err: any) {
      // If fetching profile fails, fallback to AuthContext user if present
      if (authUser) {
        setProfile(authUser);
      } else {
        const msg = err?.response?.data?.message || err?.message || 'Failed to load user profile';
        setError(msg);
      }
    } finally {
      setIsLoadingProfile(false);
    }

    try {
      // Fetch user task statistics for account telemetry
      const taskPage = await taskService.getTasks({ page: 0, size: 100 });
      const tasks = taskPage.content || [];
      const totalTasks = taskPage.totalElements || tasks.length;
      const completedTasks = tasks.filter((t) => t.status === 'COMPLETED').length;
      const pendingTasks = tasks.filter((t) => t.status === 'TODO' || t.status === 'IN_PROGRESS').length;
      const overdueTasks = tasks.filter((t) => t.overdue || (t.dueDate && new Date(t.dueDate) < new Date() && t.status !== 'COMPLETED')).length;

      const completionRate = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

      setStats({
        totalTasks,
        completedTasks,
        pendingTasks,
        overdueTasks,
        completionRate,
      });
    } catch (err) {
      // If task statistics fail to load, default to zero metrics without crashing page
      setStats({
        totalTasks: 0,
        completedTasks: 0,
        pendingTasks: 0,
        overdueTasks: 0,
        completionRate: 0,
      });
    } finally {
      setIsLoadingStats(false);
    }
  }, [authUser, updateUser]);

  useEffect(() => {
    fetchProfileAndStats();
  }, [fetchProfileAndStats]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchProfileAndStats();
    setIsRefreshing(false);
  };

  const activeUser = profile || authUser;

  if (error && !activeUser) {
    return (
      <div className="py-12">
        <ErrorState
          title="Unable to load developer profile"
          message={error}
          onRetry={fetchProfileAndStats}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12 animate-in fade-in duration-200">
      {/* Profile Header */}
      <ProfileHeader
        onEditProfile={() => setIsEditModalOpen(true)}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
      />

      {/* Main Profile Avatar & Banner */}
      {isLoadingProfile && !activeUser ? (
        <Card className="p-8 space-y-4" data-testid="profile-skeleton">
          <div className="flex items-center gap-4">
            <Skeleton className="w-20 h-20 rounded-2xl" />
            <div className="space-y-2">
              <Skeleton className="h-6 w-48 rounded" />
              <Skeleton className="h-4 w-32 rounded" />
            </div>
          </div>
        </Card>
      ) : activeUser ? (
        <ProfileAvatar user={activeUser} onEditClick={() => setIsEditModalOpen(true)} />
      ) : null}

      {/* Profile Account Statistics */}
      <ProfileStats
        stats={stats}
        isLoading={isLoadingStats}
        userCreatedAt={activeUser?.createdAt}
      />

      {/* User Detailed Information */}
      {activeUser && <ProfileInformation user={activeUser} />}

      {/* Edit Profile Modal */}
      {activeUser && (
        <EditProfileModal
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          user={activeUser}
          onProfileUpdated={(updated) => {
            setProfile(updated);
            updateUser(updated);
          }}
        />
      )}
    </div>
  );
};
