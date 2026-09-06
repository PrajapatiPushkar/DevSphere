import React, { useState } from 'react';
import { User } from '../../types';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { ShieldCheck, Calendar, Camera } from 'lucide-react';

export interface ProfileAvatarProps {
  user: User;
  onEditClick?: () => void;
}

export const ProfileAvatar: React.FC<ProfileAvatarProps> = ({ user, onEditClick }) => {
  const [imgError, setImgError] = useState(false);

  const getInitials = () => {
    if (user.firstName && user.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    if (user.displayName) {
      const parts = user.displayName.trim().split(' ');
      if (parts.length >= 2) {
        return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
      }
      return user.displayName.substring(0, 2).toUpperCase();
    }
    if (user.email) {
      return user.email.substring(0, 2).toUpperCase();
    }
    return 'DS';
  };

  const formattedJoinedDate = user.createdAt
    ? new Date(user.createdAt).toLocaleDateString('en-US', {
        month: 'short',
        year: 'numeric',
      })
    : 'Member';

  return (
    <Card className="relative overflow-hidden p-6 sm:p-8">
      {/* Background Subtle Gradient Glow */}
      <div className="absolute top-0 right-0 -mt-8 -mr-8 w-64 h-64 bg-brand-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
          {/* Avatar Container with Fallback */}
          <div className="relative group">
            <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center text-slate-100 font-bold text-2xl sm:text-3xl shadow-glow border-2 border-brand-400/30 shrink-0 select-none overflow-hidden">
              {/* Optional avatar image if supplied, with initials fallback */}
              {user.githubUrl && !imgError ? (
                <img
                  src={`${user.githubUrl.replace(/\/$/, '')}.png`}
                  alt={user.displayName || 'Avatar'}
                  className="w-full h-full object-cover"
                  onError={() => setImgError(true)}
                />
              ) : (
                <span>{getInitials()}</span>
              )}
            </div>

            {/* Photo upload status indicator / badge */}
            <div
              className="absolute -bottom-1.5 -right-1.5 p-1.5 rounded-full bg-slate-900 border border-slate-700 text-slate-400 text-xs shadow-md"
              title="Custom avatar uploads managed via OAuth / Gravatar"
            >
              <Camera className="w-3.5 h-3.5" />
            </div>
          </div>

          {/* User Primary Metadata */}
          <div className="space-y-1.5">
            <div className="flex flex-wrap items-center gap-2.5">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-100 tracking-tight">
                {user.displayName || `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Developer'}
              </h2>
              <Badge variant="brand" size="sm" className="font-mono">
                {user.role || 'DEVELOPER'}
              </Badge>
              <Badge variant="success" size="sm" dot>
                Active
              </Badge>
            </div>

            <p className="text-sm font-medium text-brand-400">
              {user.headline || user.currentRole || 'Software Engineer @ DevSphere'}
            </p>

            <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 pt-1">
              <div className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                <span>{user.email}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-slate-500" />
                <span>Joined {formattedJoinedDate}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Quick Action Button */}
        {onEditClick && (
          <button
            onClick={onEditClick}
            className="self-start md:self-center text-xs font-semibold text-brand-400 hover:text-brand-300 bg-brand-500/10 hover:bg-brand-500/20 px-3 py-2 rounded-xl border border-brand-500/20 transition flex items-center gap-1.5"
          >
            Update Info
          </button>
        )}
      </div>
    </Card>
  );
};
