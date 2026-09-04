import React, { useState } from 'react';
import { PageHeader } from '../components/layout/PageHeader';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Table, Column } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Alert } from '../components/common/Alert';
import { EmptyState } from '../components/common/EmptyState';
import { ErrorState } from '../components/common/ErrorState';
import { useToast } from '../context/ToastContext';
import { TaskSummary, SystemMetricSummary } from '../types';
import {
  Activity,
  Cpu,
  Layers,
  Zap,
  Plus,
  RefreshCw,
  MoreVertical,
  CheckCircle2,
  AlertTriangle,
  FileCheck,
  Server,
  Play,
  Sliders,
  Filter
} from 'lucide-react';
import { Dropdown } from '../components/ui/Dropdown';

export const DashboardPage: React.FC = () => {
  const { showToast } = useToast();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [viewState, setViewState] = useState<'normal' | 'empty' | 'error'>('normal');

  const metrics: SystemMetricSummary[] = [
    { label: 'Cluster Throughput', value: '1,420 req/s', change: '+12.4%', trend: 'up' },
    { label: 'Kafka Event Outbox', value: '0 Pending', change: '100% Synced', trend: 'up' },
    { label: 'Redis Cache Hit Rate', value: '98.6%', change: '+1.2%', trend: 'up' },
    { label: 'P95 API Latency', value: '42 ms', change: '-5ms SLA', trend: 'up' },
  ];

  const sampleTasks: TaskSummary[] = [
    { id: 101, title: 'Configure Prometheus scrape jobs for user-service', priority: 'HIGH', status: 'COMPLETED', dueDate: '2026-09-04' },
    { id: 102, title: 'Verify Kubernetes Pod Disruption Budget minAvailable=1', priority: 'URGENT', status: 'IN_PROGRESS', dueDate: '2026-09-05' },
    { id: 103, title: 'Audit Micrometer custom counters for authentication attempts', priority: 'MEDIUM', status: 'TODO', dueDate: '2026-09-06' },
    { id: 104, title: 'Verify HPA CPU & Memory scaling triggers', priority: 'LOW', status: 'COMPLETED', dueDate: '2026-09-03' },
  ];

  const columns: Column<TaskSummary>[] = [
    {
      header: 'Task Summary',
      accessor: (row) => (
        <div>
          <span className="font-semibold text-slate-100">{row.title}</span>
          <p className="text-[10px] text-slate-400 font-mono mt-0.5">ID: TASK-{row.id}</p>
        </div>
      ),
    },
    {
      header: 'Priority',
      accessor: (row) => {
        const variants: Record<string, 'danger' | 'warning' | 'info' | 'neutral'> = {
          URGENT: 'danger',
          HIGH: 'warning',
          MEDIUM: 'info',
          LOW: 'neutral',
        };
        return <Badge variant={variants[row.priority] || 'neutral'}>{row.priority}</Badge>;
      },
    },
    {
      header: 'Status',
      accessor: (row) => {
        const variants: Record<string, 'success' | 'warning' | 'info' | 'neutral'> = {
          COMPLETED: 'success',
          IN_PROGRESS: 'warning',
          TODO: 'info',
          CANCELLED: 'neutral',
        };
        return <Badge variant={variants[row.status] || 'neutral'} dot>{row.status}</Badge>;
      },
    },
    {
      header: 'Due Date',
      accessor: (row) => <span className="font-mono text-xs text-slate-400">{row.dueDate}</span>,
    },
    {
      header: 'Actions',
      accessor: (row) => (
        <Dropdown
          align="right"
          trigger={
            <button className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition">
              <MoreVertical className="w-4 h-4" />
            </button>
          }
          items={[
            {
              id: 'edit',
              label: 'Update Task',
              onClick: () => showToast(`Selected Task-${row.id} for editing`, 'info'),
            },
            {
              id: 'complete',
              label: 'Mark Completed',
              onClick: () => showToast(`Task-${row.id} completed successfully`, 'success'),
            },
          ]}
        />
      ),
    },
  ];

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <PageHeader
        title="Production Dashboard"
        description="DevSphere cloud-native microservices architecture overview, platform health, and component design system showcase."
        breadcrumbs={[{ label: 'Console', href: '#' }, { label: 'Overview' }]}
        actions={
          <>
            <Button
              variant="outline"
              size="sm"
              leftIcon={<RefreshCw className="w-4 h-4" />}
              onClick={() => showToast('Refreshed cluster telemetry data', 'info')}
            >
              Refresh Metrics
            </Button>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Plus className="w-4 h-4" />}
              onClick={() => setIsModalOpen(true)}
            >
              New Task
            </Button>
          </>
        }
      />

      {/* Alert Banner */}
      <Alert type="success" title="Cluster Health Normal">
        All 5 microservices (API Gateway, Auth Service, User Service, Config Server, Service Discovery) are running with 100% readiness probe success across Kubernetes nodes.
      </Alert>

      {/* Stat Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {metrics.map((m, idx) => (
          <Card key={idx} glass className="p-5 space-y-2">
            <div className="flex items-center justify-between text-slate-400 text-xs font-semibold uppercase tracking-wider">
              <span>{m.label}</span>
              <Activity className="w-4 h-4 text-brand-400" />
            </div>
            <div className="text-2xl font-extrabold text-slate-100">{m.value}</div>
            <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{m.change}</span>
            </div>
          </Card>
        ))}
      </div>

      {/* Component State Demonstrator Controller */}
      <Card className="p-4 bg-slate-900/60 border border-slate-800 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
          <Sliders className="w-4 h-4 text-brand-400" />
          <span>Interactive Component State Showcase:</span>
        </div>
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant={viewState === 'normal' ? 'primary' : 'ghost'}
            onClick={() => setViewState('normal')}
          >
            Normal View
          </Button>
          <Button
            size="sm"
            variant={viewState === 'empty' ? 'primary' : 'ghost'}
            onClick={() => setViewState('empty')}
          >
            Empty State
          </Button>
          <Button
            size="sm"
            variant={viewState === 'error' ? 'primary' : 'ghost'}
            onClick={() => setViewState('error')}
          >
            Error State
          </Button>
        </div>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={() => showToast('Action performed successfully', 'success', 'Success Toast')}>
            Toast Demo
          </Button>
        </div>
      </Card>

      {/* Main Content Area based on View State */}
      {viewState === 'empty' ? (
        <EmptyState
          title="No tasks registered yet"
          description="Create your first task in DevSphere to track microservice development progress and deployment phases."
          actionLabel="Create First Task"
          onAction={() => setIsModalOpen(true)}
        />
      ) : viewState === 'error' ? (
        <ErrorState
          title="Failed to load telemetry"
          message="Could not reach API Gateway at http://localhost:8080. Verify that microservice pods are running."
          onRetry={() => {
            setViewState('normal');
            showToast('Reconnected to API Gateway', 'success');
          }}
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Table Container */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-slate-100 tracking-tight">Active Platform Tasks</h2>
              <Badge variant="brand">4 Items</Badge>
            </div>
            <Table
              columns={columns}
              data={sampleTasks}
              keyExtractor={(item) => item.id}
              emptyMessage="No tasks found matching filter"
            />
          </div>

          {/* Microservice Architecture Summary Card */}
          <div className="space-y-4">
            <h2 className="text-base font-bold text-slate-100 tracking-tight">Services Overview</h2>
            <Card glass className="divide-y divide-slate-800/80">
              {[
                { name: 'api-gateway', port: 8080, status: 'UP', type: 'Gateway' },
                { name: 'auth-service', port: 8081, status: 'UP', type: 'Auth & Outbox' },
                { name: 'user-service', port: 8082, status: 'UP', type: 'User & Tasks' },
                { name: 'config-server', port: 8888, status: 'UP', type: 'Config' },
                { name: 'service-discovery', port: 8761, status: 'UP', type: 'Eureka' },
              ].map((svc, idx) => (
                <div key={idx} className="p-4 flex items-center justify-between text-xs">
                  <div className="space-y-0.5">
                    <p className="font-semibold text-slate-200">{svc.name}</p>
                    <p className="text-[10px] text-slate-400 font-mono">Port :{svc.port} • {svc.type}</p>
                  </div>
                  <Badge variant="success" size="sm" dot>
                    {svc.status}
                  </Badge>
                </div>
              ))}
            </Card>
          </div>
        </div>
      )}

      {/* New Task Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create New Platform Task"
        description="Add a task to the DevSphere microservices task registry."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                setIsModalOpen(false);
                showToast('Task created successfully in User Service', 'success', 'Task Saved');
              }}
            >
              Create Task
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input label="Task Title" placeholder="e.g. Implement Redis Cache Eviction" required />
          <Select
            label="Priority Level"
            options={[
              { value: 'LOW', label: 'Low Priority' },
              { value: 'MEDIUM', label: 'Medium Priority' },
              { value: 'HIGH', label: 'High Priority' },
              { value: 'URGENT', label: 'Urgent' },
            ]}
          />
          <Input label="Due Date" type="date" defaultValue="2026-09-10" />
        </div>
      </Modal>
    </div>
  );
};
