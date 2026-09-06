import React, { useState, useEffect } from 'react';
import { User, UpdateUserProfileInput } from '../../types';
import { userService } from '../../services/userService';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Textarea } from '../ui/Textarea';
import { Button } from '../ui/Button';
import { User as UserIcon, Briefcase, MapPin, Phone, Github, Linkedin, Globe, Hash } from 'lucide-react';

export interface EditProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  user: User;
  onProfileUpdated?: (updatedUser: User) => void;
}

export const EditProfileModal: React.FC<EditProfileModalProps> = ({
  isOpen,
  onClose,
  user,
  onProfileUpdated,
}) => {
  const { updateUser } = useAuth();
  const { showToast } = useToast();

  const [formData, setFormData] = useState<UpdateUserProfileInput>({
    firstName: '',
    lastName: '',
    displayName: '',
    headline: '',
    bio: '',
    location: '',
    phoneNumber: '',
    githubUrl: '',
    linkedinUrl: '',
    portfolioUrl: '',
    currentRole: '',
    yearsOfExperience: undefined,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen && user) {
      setFormData({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        displayName: user.displayName || '',
        headline: user.headline || '',
        bio: user.bio || '',
        location: user.location || '',
        phoneNumber: user.phoneNumber || '',
        githubUrl: user.githubUrl || '',
        linkedinUrl: user.linkedinUrl || '',
        portfolioUrl: user.portfolioUrl || '',
        currentRole: user.currentRole || '',
        yearsOfExperience: user.yearsOfExperience ?? undefined,
      });
      setErrors({});
    }
  }, [isOpen, user]);

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (formData.firstName && formData.firstName.length > 100) {
      newErrors.firstName = 'First name must not exceed 100 characters';
    }
    if (formData.lastName && formData.lastName.length > 100) {
      newErrors.lastName = 'Last name must not exceed 100 characters';
    }
    if (formData.displayName && formData.displayName.length > 200) {
      newErrors.displayName = 'Display name must not exceed 200 characters';
    }
    if (formData.headline && formData.headline.length > 250) {
      newErrors.headline = 'Headline must not exceed 250 characters';
    }
    if (formData.bio && formData.bio.length > 2000) {
      newErrors.bio = 'Bio must not exceed 2000 characters';
    }

    const urlPattern = /^(https?:\/\/.+)?$/;
    if (formData.githubUrl && !urlPattern.test(formData.githubUrl)) {
      newErrors.githubUrl = 'Must be a valid HTTP or HTTPS URL (e.g. https://github.com/username)';
    }
    if (formData.linkedinUrl && !urlPattern.test(formData.linkedinUrl)) {
      newErrors.linkedinUrl = 'Must be a valid HTTP or HTTPS URL (e.g. https://linkedin.com/in/username)';
    }
    if (formData.portfolioUrl && !urlPattern.test(formData.portfolioUrl)) {
      newErrors.portfolioUrl = 'Must be a valid HTTP or HTTPS URL (e.g. https://example.com)';
    }

    if (
      formData.yearsOfExperience !== undefined &&
      formData.yearsOfExperience !== null &&
      Number(formData.yearsOfExperience) < 0
    ) {
      newErrors.yearsOfExperience = 'Years of experience cannot be negative';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const payload: UpdateUserProfileInput = {
        ...formData,
        yearsOfExperience:
          formData.yearsOfExperience !== undefined && formData.yearsOfExperience !== null
            ? Number(formData.yearsOfExperience)
            : undefined,
      };

      const updated = await userService.updateMyProfile(payload);
      updateUser(updated);
      if (onProfileUpdated) onProfileUpdated(updated);
      showToast('Profile updated successfully', 'success');
      onClose();
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Failed to update profile';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Edit Developer Profile"
      description="Update your personal details, role credentials, and public developer links."
      maxWidth="lg"
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleSubmit}
            isLoading={isSubmitting}
            disabled={isSubmitting}
          >
            Save Profile
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="First Name"
            placeholder="Pushkar"
            value={formData.firstName || ''}
            onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
            error={errors.firstName}
            leftIcon={<UserIcon className="w-4 h-4" />}
          />
          <Input
            label="Last Name"
            placeholder="Prajapati"
            value={formData.lastName || ''}
            onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
            error={errors.lastName}
            leftIcon={<UserIcon className="w-4 h-4" />}
          />
        </div>

        <Input
          label="Display Name"
          placeholder="Pushkar Prajapati"
          value={formData.displayName || ''}
          onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
          error={errors.displayName}
          helperText="Name displayed across header and dashboard cards"
          leftIcon={<UserIcon className="w-4 h-4" />}
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Current Role"
            placeholder="Senior Platform Engineer"
            value={formData.currentRole || ''}
            onChange={(e) => setFormData({ ...formData, currentRole: e.target.value })}
            leftIcon={<Briefcase className="w-4 h-4" />}
          />
          <Input
            label="Years of Experience"
            type="number"
            placeholder="5"
            value={formData.yearsOfExperience ?? ''}
            onChange={(e) =>
              setFormData({
                ...formData,
                yearsOfExperience: e.target.value === '' ? undefined : Number(e.target.value),
              })
            }
            error={errors.yearsOfExperience}
            leftIcon={<Hash className="w-4 h-4" />}
          />
        </div>

        <Input
          label="Professional Headline"
          placeholder="Building scalable microservices & Kubernetes platforms"
          value={formData.headline || ''}
          onChange={(e) => setFormData({ ...formData, headline: e.target.value })}
          error={errors.headline}
        />

        <Textarea
          label="Bio / Summary"
          placeholder="Share your technical interests, tech stack, or career highlights..."
          rows={3}
          value={formData.bio || ''}
          onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
          error={errors.bio}
        />

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Location"
            placeholder="San Francisco, CA"
            value={formData.location || ''}
            onChange={(e) => setFormData({ ...formData, location: e.target.value })}
            leftIcon={<MapPin className="w-4 h-4" />}
          />
          <Input
            label="Phone Number"
            placeholder="+1 (555) 000-0000"
            value={formData.phoneNumber || ''}
            onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
            leftIcon={<Phone className="w-4 h-4" />}
          />
        </div>

        <div className="pt-2 border-t border-slate-800 space-y-3">
          <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
            Online Profiles & External Links
          </h4>

          <Input
            label="GitHub URL"
            placeholder="https://github.com/prajapatipushkar"
            value={formData.githubUrl || ''}
            onChange={(e) => setFormData({ ...formData, githubUrl: e.target.value })}
            error={errors.githubUrl}
            leftIcon={<Github className="w-4 h-4" />}
          />

          <Input
            label="LinkedIn URL"
            placeholder="https://linkedin.com/in/pushkarprajapati"
            value={formData.linkedinUrl || ''}
            onChange={(e) => setFormData({ ...formData, linkedinUrl: e.target.value })}
            error={errors.linkedinUrl}
            leftIcon={<Linkedin className="w-4 h-4 text-blue-400" />}
          />

          <Input
            label="Portfolio Website URL"
            placeholder="https://devsphere.io/pushkar"
            value={formData.portfolioUrl || ''}
            onChange={(e) => setFormData({ ...formData, portfolioUrl: e.target.value })}
            error={errors.portfolioUrl}
            leftIcon={<Globe className="w-4 h-4 text-emerald-400" />}
          />
        </div>
      </form>
    </Modal>
  );
};
