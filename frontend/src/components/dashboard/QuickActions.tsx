import React from 'react';
import { Card } from '../ui/Card';
import { Button } from '../ui/Button';
import { Link } from 'react-router-dom';
import { Plus, CheckSquare, User as UserIcon, Zap } from 'lucide-react';

export interface QuickActionsProps {
  onNewTaskClick: () => void;
}

export const QuickActions: React.FC<QuickActionsProps> = ({ onNewTaskClick }) => {
  return (
    <Card glass className="p-6 space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-2 rounded-xl bg-brand-500/10 border border-brand-500/20 text-brand-400">
          <Zap className="w-5 h-5" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-100 tracking-tight">Quick Actions</h3>
          <p className="text-xs text-slate-400">Fast shortcuts for common operations</p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
        <Button
          variant="primary"
          size="md"
          leftIcon={<Plus className="w-4 h-4" />}
          onClick={onNewTaskClick}
          className="w-full justify-center"
        >
          New Task
        </Button>

        <Link to="/tasks" className="w-full">
          <Button
            variant="outline"
            size="md"
            leftIcon={<CheckSquare className="w-4 h-4" />}
            className="w-full justify-center"
          >
            View Tasks
          </Button>
        </Link>

        <Link to="/settings" className="w-full">
          <Button
            variant="secondary"
            size="md"
            leftIcon={<UserIcon className="w-4 h-4" />}
            className="w-full justify-center"
          >
            Profile Settings
          </Button>
        </Link>
      </div>
    </Card>
  );
};
