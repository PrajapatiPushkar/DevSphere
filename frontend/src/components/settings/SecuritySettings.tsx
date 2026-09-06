import React from 'react';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Alert } from '../common/Alert';
import { Shield, Key, Lock, CheckCircle, Info } from 'lucide-react';

export const SecuritySettings: React.FC = () => {
  return (
    <div className="space-y-6">
      <Card className="p-6 space-y-6">
        <div className="border-b border-slate-800 pb-4 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <Shield className="w-4 h-4 text-brand-400" />
              Password & Security Governance
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Active authentication session security and credential management rules.
            </p>
          </div>
          <Badge variant="success" dot>
            Session Active
          </Badge>
        </div>

        {/* Security Informational Notice */}
        <Alert type="info" title="Central Security Policy">
          Password changes and credential resets are managed securely via DevSphere Auth Service & Corporate SSO. Password updates are currently restricted from self-service frontend forms to enforce enterprise audit logging.
        </Alert>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
              <Key className="w-4 h-4 text-brand-400" />
              Authentication Mechanism
            </div>
            <p className="text-xs text-slate-400">
              JWT Bearer Tokens with RSA-256 signatures issued by <code className="text-brand-300">auth-service</code>.
            </p>
            <div className="pt-2">
              <Badge variant="neutral" size="sm" className="font-mono">
                BCrypt Encrypted
              </Badge>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
              <Lock className="w-4 h-4 text-emerald-400" />
              API Gateway Enforcer
            </div>
            <p className="text-xs text-slate-400">
              Header injection via <code className="text-emerald-300">X-Authenticated-User-Id</code> header validation on API Gateway.
            </p>
            <div className="pt-2">
              <Badge variant="success" size="sm" className="font-mono">
                Kubernetes Ingress Secure
              </Badge>
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-800 space-y-3">
          <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Security Checklist & Session Status
          </h4>
          <div className="space-y-2 text-xs text-slate-300">
            <div className="flex items-center gap-2 p-3 rounded-lg bg-slate-900/40 border border-slate-800/60">
              <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>Multi-tier JWT signature validation enabled on Spring Boot Security context.</span>
            </div>
            <div className="flex items-center gap-2 p-3 rounded-lg bg-slate-900/40 border border-slate-800/60">
              <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>Zero plain-text credentials stored in browser storage or log output.</span>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};
