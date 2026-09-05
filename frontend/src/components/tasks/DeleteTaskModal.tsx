import React, { useState } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Task } from '../../types';
import { AlertTriangle } from 'lucide-react';

export interface DeleteTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  task: Task | null;
  onConfirmDelete: (taskId: number) => Promise<void>;
}

export const DeleteTaskModal: React.FC<DeleteTaskModalProps> = ({
  isOpen,
  onClose,
  task,
  onConfirmDelete,
}) => {
  const [isDeleting, setIsDeleting] = useState(false);

  if (!task) return null;

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await onConfirmDelete(task.id);
      onClose();
    } catch {
      // Handled by parent toast
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Delete Task"
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose} disabled={isDeleting}>
            Cancel
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={handleDelete}
            isLoading={isDeleting}
          >
            Delete Task
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="flex items-center gap-3 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300">
          <AlertTriangle className="w-6 h-6 text-rose-400 shrink-0" />
          <div className="text-xs space-y-0.5">
            <p className="font-semibold text-slate-100">Are you sure you want to delete this task?</p>
            <p className="text-slate-400">This action will archive/delete TASK-{task.id} from your workspace repository.</p>
          </div>
        </div>

        <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <span className="text-xs font-bold text-slate-200">{task.title}</span>
          {task.description && (
            <p className="text-xs text-slate-400 line-clamp-2">{task.description}</p>
          )}
        </div>
      </div>
    </Modal>
  );
};
