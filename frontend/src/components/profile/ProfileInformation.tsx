import React from 'react';
import { User } from '../../types';
import { Card } from '../ui/Card';
import { Mail, Briefcase, MapPin, Phone, Github, Linkedin, Globe, UserCheck, Clock, Award } from 'lucide-react';

export interface ProfileInformationProps {
  user: User;
}

export const ProfileInformation: React.FC<ProfileInformationProps> = ({ user }) => {
  const getFullName = () => {
    if (user.firstName || user.lastName) {
      return `${user.firstName || ''} ${user.lastName || ''}`.trim();
    }
    return user.displayName || 'N/A';
  };

  const infoFields = [
    {
      label: 'Full Name',
      value: getFullName(),
      icon: <UserCheck className="w-4 h-4 text-slate-400" />,
    },
    {
      label: 'Email Address',
      value: user.email || 'N/A',
      icon: <Mail className="w-4 h-4 text-slate-400" />,
    },
    {
      label: 'Current Role',
      value: user.currentRole || user.headline || 'Software Engineer',
      icon: <Briefcase className="w-4 h-4 text-slate-400" />,
    },
    {
      label: 'Years of Experience',
      value: user.yearsOfExperience != null ? `${user.yearsOfExperience} Years` : 'Not specified',
      icon: <Clock className="w-4 h-4 text-slate-400" />,
    },
    {
      label: 'Location',
      value: user.location || 'Not specified',
      icon: <MapPin className="w-4 h-4 text-slate-400" />,
    },
    {
      label: 'Phone Number',
      value: user.phoneNumber || 'Not specified',
      icon: <Phone className="w-4 h-4 text-slate-400" />,
    },
  ];

  const socialLinks = [
    {
      label: 'GitHub Profile',
      url: user.githubUrl,
      icon: <Github className="w-4 h-4 text-slate-300" />,
    },
    {
      label: 'LinkedIn Profile',
      url: user.linkedinUrl,
      icon: <Linkedin className="w-4 h-4 text-blue-400" />,
    },
    {
      label: 'Portfolio Website',
      url: user.portfolioUrl,
      icon: <Globe className="w-4 h-4 text-emerald-400" />,
    },
  ].filter((item) => item.url);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Primary User Information Details (2 columns on lg) */}
      <Card className="lg:col-span-2 p-6 space-y-6">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Award className="w-4 h-4 text-brand-400" />
            Developer Identity & Credentials
          </h3>
          <span className="text-xs text-slate-500 font-mono">ID: {user.userId || user.id || 'N/A'}</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {infoFields.map((field, idx) => (
            <div key={idx} className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-1">
              <div className="flex items-center gap-2 text-xs font-medium text-slate-400">
                {field.icon}
                <span>{field.label}</span>
              </div>
              <p className="text-sm font-semibold text-slate-200 truncate">{field.value}</p>
            </div>
          ))}
        </div>

        {/* Bio Section */}
        {user.bio && (
          <div className="pt-2 border-t border-slate-800">
            <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">About & Bio</h4>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed whitespace-pre-line bg-slate-900/40 p-4 rounded-xl border border-slate-800/80">
              {user.bio}
            </p>
          </div>
        )}
      </Card>

      {/* External Links & Metadata Sidebar Card (1 column on lg) */}
      <Card className="p-6 space-y-6">
        <div className="border-b border-slate-800 pb-4">
          <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Globe className="w-4 h-4 text-brand-400" />
            Online Presence & Links
          </h3>
        </div>

        {socialLinks.length > 0 ? (
          <div className="space-y-3">
            {socialLinks.map((link, idx) => (
              <a
                key={idx}
                href={link.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between p-3 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 text-xs text-slate-200 transition group"
              >
                <div className="flex items-center gap-2.5 truncate">
                  {link.icon}
                  <span className="truncate font-medium">{link.label}</span>
                </div>
                <span className="text-[10px] text-brand-400 group-hover:underline font-mono">View →</span>
              </a>
            ))}
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-slate-900/40 border border-slate-800 text-center space-y-1">
            <p className="text-xs text-slate-400">No external links added yet.</p>
            <p className="text-[11px] text-slate-500">Edit your profile to add GitHub, LinkedIn, or Portfolio URLs.</p>
          </div>
        )}

        <div className="pt-4 border-t border-slate-800 space-y-2">
          <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Account Governance</h4>
          <div className="text-xs text-slate-400 space-y-1.5 font-mono">
            <div className="flex justify-between">
              <span>Security Tier:</span>
              <span className="text-emerald-400">JWT Authenticated</span>
            </div>
            <div className="flex justify-between">
              <span>Service Scope:</span>
              <span className="text-slate-300">user-service v1</span>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};
