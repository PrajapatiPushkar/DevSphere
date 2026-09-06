import React, { useState } from 'react';
import { User, UpdateUserProfileInput } from '../../types';
import { userService } from '../../services/userService';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Card } from '../ui/Card';
import { Input } from '../ui/Input';
import { Textarea } from '../ui/Textarea';
import { Button } from '../ui/Button';
import { Alert } from '../common/Alert';
import { User as UserIcon, Mail, Briefcase, MapPin, Phone, Info } from 'lucide-react';

export interface AccountSettingsProps {
  user: User;
}

export const AccountSettings: React.FC<AccountSettingsProps> = ({ user }) => {
  const { updateUser } = useAuth();
  const { showToast } = useToast();

  const [formData, setFormData] = useState<UpdateUserProfileInput>({
    firstName: user.firstName || '',
    lastName: user.lastName || '',
    displayName: user.displayName || '',
    headline: user.headline || '',
    currentRole: user.currentRole || '',
    bio: user.bio || '',
    location: user.location || '',
    phoneNumber: user.phoneNumber || '',
  });

  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      const updated = await userService.updateMyProfile(formData);
      updateUser(updated);
      showToast('Account settings saved successfully', 'success');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to save account settings';
      showToast(msg, 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Card className="p-6 space-y-6">
      <div className="border-b border-slate-800 pb-4">
        <h3 className="text-base font-bold text-slate-100">Personal Information</h3>
        <p className="text-xs text-slate-400 mt-1">
          Manage your identity and profile metadata across DevSphere microservices.
        </p>
      </div>

      {/* Read-Only Email Notice */}
      <Alert type="info" title="Email Address Governance">
        Your account email ({user.email || 'N/A'}) is managed by DevSphere Central Authentication Service. Email address modification is currently restricted to system administrators.
      </Alert>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="First Name"
            value={formData.firstName || ''}
            onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
            leftIcon={<UserIcon className="w-4 h-4" />}
          />
          <Input
            label="Last Name"
            value={formData.lastName || ''}
            onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
            leftIcon={<UserIcon className="w-4 h-4" />}
          />
        </div>

        <Input
          label="Display Name"
          value={formData.displayName || ''}
          onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
          helperText="Visible to team members and collaborators"
          leftIcon={<UserIcon className="w-4 h-4" />}
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Account Email (Read Only)"
            value={user.email || ''}
            disabled
            leftIcon={<Mail className="w-4 h-4 text-slate-500" />}
          />
          <Input
            label="Current Role"
            value={formData.currentRole || ''}
            onChange={(e) => setFormData({ ...formData, currentRole: e.target.value })}
            leftIcon={<Briefcase className="w-4 h-4" />}
          />
        </div>

        <Input
          label="Professional Headline"
          value={formData.headline || ''}
          onChange={(e) => setFormData({ ...formData, headline: e.target.value })}
        />

        <Textarea
          label="Bio & Summary"
          rows={3}
          value={formData.bio || ''}
          onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Location"
            value={formData.location || ''}
            onChange={(e) => setFormData({ ...formData, location: e.target.value })}
            leftIcon={<MapPin className="w-4 h-4" />}
          />
          <Input
            label="Phone Number"
            value={formData.phoneNumber || ''}
            onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
            leftIcon={<Phone className="w-4 h-4" />}
          />
        </div>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <Button variant="primary" size="sm" type="submit" isLoading={isSaving} disabled={isSaving}>
            Save Changes
          </Button>
        </div>
      </form>
    </Card>
  );
};
