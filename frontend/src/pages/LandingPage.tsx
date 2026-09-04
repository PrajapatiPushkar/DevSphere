import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Card, CardContent } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Terminal, Cpu, ShieldCheck, Zap, ArrowRight, Layers, Database, Activity, CheckCircle2 } from 'lucide-react';

export const LandingPage: React.FC = () => {
  const features = [
    {
      title: 'Spring Boot Microservices',
      description: 'Decoupled services for Auth, User Profiles, Tasks, and Resumes with API Gateway routing.',
      icon: <Cpu className="w-6 h-6 text-brand-400" />,
    },
    {
      title: 'Kubernetes Self-Healing',
      description: 'Probes, rolling updates, autoscaling (HPA), and pod disruption budgets for 99.99% uptime.',
      icon: <ShieldCheck className="w-6 h-6 text-emerald-400" />,
    },
    {
      title: 'Production Observability',
      description: 'Prometheus metrics scraping and Grafana dashboards for full system visibility.',
      icon: <Activity className="w-6 h-6 text-purple-400" />,
    },
    {
      title: 'Event-Driven Pipeline',
      description: 'Kafka event bus with Transactional Outbox pattern and guaranteed idempotency.',
      icon: <Zap className="w-6 h-6 text-amber-400" />,
    },
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between selection:bg-brand-500 selection:text-white">
      {/* Header Navigation */}
      <header className="h-20 border-b border-slate-800/80 px-6 max-w-7xl mx-auto w-full flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-400 flex items-center justify-center shadow-glow">
            <Terminal className="w-5 h-5 text-white" />
          </div>
          <span className="text-xl font-bold tracking-tight text-slate-100">DevSphere</span>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/login">
            <Button variant="ghost" size="sm">
              Sign In
            </Button>
          </Link>
          <Link to="/dashboard">
            <Button variant="primary" size="sm" rightIcon={<ArrowRight className="w-4 h-4" />}>
              Launch Console
            </Button>
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-6 py-16 md:py-24 space-y-20">
        <div className="text-center max-w-3xl mx-auto space-y-6">
          <Badge variant="brand" dot className="px-3 py-1 text-xs">
            Microservices Developer Platform
          </Badge>
          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-slate-100 leading-tight">
            Build, Deploy & Observe <br />
            <span className="bg-gradient-to-r from-brand-400 via-indigo-300 to-purple-400 bg-clip-text text-transparent">
              Cloud-Native SaaS
            </span>
          </h1>
          <p className="text-base sm:text-lg text-slate-400 leading-relaxed max-w-2xl mx-auto">
            DevSphere combines Spring Boot microservices, Kafka event pipelines, Redis caching, Kubernetes orchestrations, and a modern React design system into a full-stack platform.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <Link to="/dashboard">
              <Button size="lg" variant="primary" rightIcon={<ArrowRight className="w-5 h-5" />}>
                Explore Dashboard
              </Button>
            </Link>
            <Link to="/register">
              <Button size="lg" variant="outline">
                Create Free Account
              </Button>
            </Link>
          </div>
        </div>

        {/* Feature Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((f, i) => (
            <Card key={i} glass className="p-6 space-y-3 hover:border-brand-500/40 transition">
              <div className="p-3 bg-slate-900/80 rounded-xl w-fit border border-slate-800">{f.icon}</div>
              <h3 className="text-base font-semibold text-slate-100">{f.title}</h3>
              <p className="text-xs text-slate-400 leading-relaxed">{f.description}</p>
            </Card>
          ))}
        </div>

        {/* Stack Overview Banner */}
        <Card className="glass-panel p-8 sm:p-10 border border-slate-800 rounded-3xl">
          <div className="flex flex-col lg:flex-row items-center justify-between gap-8">
            <div className="space-y-3 max-w-xl">
              <Badge variant="info">Full-Stack SaaS Architecture</Badge>
              <h2 className="text-2xl sm:text-3xl font-bold text-slate-100">
                Engineered for High-Scale & Reliability
              </h2>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                Featuring API Gateway routing, Eureka service discovery, distributed rate limiting, outbox event publishing, and Prometheus metrics scraping out of the box.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4 w-full lg:w-auto">
              {['Spring Boot 3.2', 'React + Vite', 'Tailwind CSS', 'Kubernetes', 'Apache Kafka', 'Redis Cache'].map((item, idx) => (
                <div key={idx} className="flex items-center gap-2 text-xs font-medium text-slate-200 bg-slate-900/80 border border-slate-800 px-3.5 py-2.5 rounded-xl">
                  <CheckCircle2 className="w-4 h-4 text-brand-400 shrink-0" />
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-8 px-6 text-center text-xs text-slate-500">
        <p>© 2026 DevSphere Microservices Platform. All rights reserved. Lesson 71 Design System.</p>
      </footer>
    </div>
  );
};
