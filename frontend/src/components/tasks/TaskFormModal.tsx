import React, { useState, useEffect } from 'react';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Textarea } from '../ui/Textarea';
import { Button } from '../ui/Button';
import { Task, TaskPriority, CreateTaskInput, UpdateTaskInput } from '../../types';

export interface TaskFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  taskToEdit?: Task | null;
  onSubmitCreate: (input: CreateTaskInput) => Promise<void>;
  onSubmitEdit: (taskId: number, input: UpdateTaskInput) => Promise<void>;
}

export const TaskFormModal: React.FC<TaskFormModalProps> = ({
  isOpen,
  onClose,
  taskToEdit,
  onSubmitCreate,
  onSubmitEdit,
}) => {
  const isEditing = !!taskToEdit;

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [errors, setErrors] = useState<{ title?: string }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (taskToEdit) {
      setTitle(taskToEdit.title || '');
      setDescription(taskToEdit.description || '');
      setPriority(taskToEdit.priority || 'MEDIUM');
      if (taskToEdit.dueDate) {
        // Format ISO string to YYYY-MM-DD for date input
        const dateObj = new Date(taskToEdit.dueDate);
        if (!isNaN(dateObj.getTime())) {
          setDueDate(dateObj.toISOString().split('T')[0]);
        } else {
          setDueDate('');
        }
      } else {
        setDueDate('');
      }
    } else {
      setTitle('');
      setDescription('');
      setPriority('MEDIUM');
      setDueDate('');
    }
    setErrors({});
  }, [taskToEdit, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setErrors({ title: 'Task title is required' });
      return;
    }

    setIsSubmitting(true);
    setErrors({});

    const formattedDueDate = dueDate ? new Date(dueDate).toISOString() : undefined;

    try {
      if (isEditing && taskToEdit) {
        await onSubmitEdit(taskToEdit.id, {
          title: title.trim(),
          description: description.trim() || undefined,
          priority,
          dueDate: formattedDueDate,
        });
      } else {
        await onSubmitCreate({
          title: title.trim(),
          description: description.trim() || undefined,
          priority,
          dueDate: formattedDueDate,
        });
      }
      onClose();
    } catch {
      // Error handling managed by parent via toast
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? `Edit Task (TASK-${taskToEdit?.id})` : 'Create New Task'}
      description={
        isEditing
          ? 'Update task details, priority, and deadline.'
          : 'Add a new microservice development task to your DevSphere workspace.'
      }
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
          >
            {isEditing ? 'Save Changes' : 'Create Task'}
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Task Title"
          placeholder="e.g. Implement Kafka Event Consumer for Auth"
          value={title}
          onChange={(e) => {
            setTitle(e.target.value);
            if (errors.title) setErrors({});
          }}
          error={errors.title}
          required
          autoFocus
        />

        <Select
          label="Priority Level"
          value={priority}
          onChange={(e) => setPriority(e.target.value as TaskPriority)}
          options={[
            { value: 'LOW', label: 'Low Priority' },
            { value: 'MEDIUM', label: 'Medium Priority' },
            { value: 'HIGH', label: 'High Priority' },
            { value: 'URGENT', label: 'Urgent' },
          ]}
        />

        <Input
          label="Due Date"
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
        />

        <Textarea
          label="Description (Optional)"
          placeholder="Add detailed task specifications, requirements, or architecture notes..."
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
        />
      </form>
    </Modal>
  );
};
