import React from 'react';
import { Card } from '../ui/Card';
import { User } from '../../types';
import { Badge } from '../ui/Badge';
import { User as UserIcon, Mail, ShieldCheck, Briefcase } from 'lucide-react';

export interface UserSummaryProps {
  user: User | null;
}

export const UserSummary: React.FC<UserSummaryProps> = ({ user }) => {
  const displayName =
    user?.displayName ||
    (user?.firstName ? `${user.firstName}${user.lastName ? ' ' + user.lastName : ''}` : null) ||
    user?.email?.split('@')[0] ||
    'Developer User';

  const initials =
    user?.firstName && user?.lastName
      ? `${user.firstName[0]}${user.lastName[0]}`.toUpperCase()
      : (displayName.substring(0, 2) || 'DS').toUpperCase();

  const role = user?.currentRole || user?.headline || 'Platform Developer';

  return (
    <Card glass className="p-6 space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-2 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
          <UserIcon className="w-5 h-5" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-100 tracking-tight">Developer Profile</h3>
          <p className="text-xs text-slate-400">Authenticated user information</p>
        </div>
      </div>

      <div className="flex items-center gap-4 p-4 rounded-xl bg-slate-900/60 border border-slate-800">
        {/* Avatar Circle with Fallback Initials */}
        <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-brand-500 to-indigo-600 text-white font-black flex items-center justify-center text-base shadow-glow shrink-0 border border-brand-400/30">
          {initials}
        </div>

        <div className="space-y-1 min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-bold text-slate-100 truncate">{displayName}</h4>
            <Badge variant="success" size="sm" dot>
              Active
            </Badge>
          </div>

          <div className="flex items-center gap-1.5 text-xs text-slate-400 truncate">
            <Mail className="w-3.5 h-3.5 text-slate-500 shrink-0" />
            <span className="truncate">{user?.email || 'user@devsphere.io'}</span>
          </div>

          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <Briefcase className="w-3.5 h-3.5 text-slate-500 shrink-0" />
            <span className="truncate">{role}</span>
          </div>
        </div>
      </div>
    </Card>
  );
};
