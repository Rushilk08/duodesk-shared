import React, { useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { Calendar, Clock, Lock, Paperclip, Trash2, Users, X } from 'lucide-react';
import {
  AssignmentItem,
  AssignmentStatus,
  Priority,
  ReminderItem,
  TaskItem,
  UserId,
  UserProfile,
  Visibility,
} from '../types/duodesk';
import { formatFileSize, formatFullDate, formatTime12h } from '../utils/formatters';
import { UserAvatar } from './UserAvatar';

export type InspectableItem =
  | { type: 'task'; item: TaskItem }
  | { type: 'reminder'; item: ReminderItem }
  | { type: 'assignment'; item: AssignmentItem };

interface ItemDetailModalProps {
  target: InspectableItem | null;
  users: Record<UserId, UserProfile>;
  onClose: () => void;
  onSaveTask?: (id: string, patch: Partial<TaskItem>) => Promise<void>;
  onDeleteTask?: (id: string) => Promise<void>;
  onSaveReminder?: (id: string, patch: Partial<ReminderItem>) => Promise<void>;
  onDeleteReminder?: (id: string) => Promise<void>;
  onSaveAssignment?: (id: string, patch: Partial<AssignmentItem>) => Promise<void>;
  onDeleteAssignment?: (id: string) => Promise<void>;
}

export function ItemDetailModal({
  target,
  users,
  onClose,
  onSaveTask,
  onDeleteTask,
  onSaveReminder,
  onDeleteReminder,
  onSaveAssignment,
  onDeleteAssignment,
}: ItemDetailModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [priority, setPriority] = useState<Priority>('medium');
  const [visibility, setVisibility] = useState<Visibility>('private');
  const [status, setStatus] = useState<AssignmentStatus>('pending');
  const [completed, setCompleted] = useState(false);

  useEffect(() => {
    if (!target) return;
    if (target.type === 'task') {
      setTitle(target.item.title);
      setDescription(target.item.description ?? '');
      setDate(target.item.dueDate);
      setTime(target.item.dueTime ?? '');
      setPriority(target.item.priority);
      setVisibility(target.item.visibility);
      setCompleted(target.item.completed);
    } else if (target.type === 'reminder') {
      setTitle(target.item.title);
      setDescription(target.item.description ?? '');
      setDate(target.item.date);
      setTime(target.item.time);
      setPriority(target.item.priority);
      setVisibility(target.item.visibility);
      setCompleted(target.item.completed);
    } else {
      setTitle(target.item.title);
      setDescription(target.item.description ?? '');
      setDate(target.item.deadline);
      setTime(target.item.deadlineTime ?? '');
      setPriority(target.item.priority);
      setVisibility(target.item.visibility);
      setStatus(target.item.status);
    }
  }, [target]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!target) return;
    if (target.type === 'task' && onSaveTask) {
      await onSaveTask(target.item.id, {
        title: title.trim(),
        description: description.trim() || undefined,
        dueDate: date,
        dueTime: time || undefined,
        priority,
        visibility,
        completed,
      });
    } else if (target.type === 'reminder' && onSaveReminder) {
      await onSaveReminder(target.item.id, {
        title: title.trim(),
        description: description.trim(),
        date,
        time,
        priority,
        visibility,
        completed,
      });
    } else if (target.type === 'assignment' && onSaveAssignment) {
      await onSaveAssignment(target.item.id, {
        title: title.trim(),
        description: description.trim(),
        deadline: date,
        deadlineTime: time || undefined,
        priority,
        visibility,
        status,
      });
    }
    onClose();
  };

  const handleDelete = async () => {
    if (!target) return;
    if (target.type === 'task' && onDeleteTask) await onDeleteTask(target.item.id);
    if (target.type === 'reminder' && onDeleteReminder) await onDeleteReminder(target.item.id);
    if (target.type === 'assignment' && onDeleteAssignment) await onDeleteAssignment(target.item.id);
    onClose();
  };

  const owner = target ? users[target.item.ownerId] : undefined;

  return (
    <AnimatePresence>
      {target && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-label="Item details"
        >
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-[#18181F]/30 backdrop-blur-xs dark:bg-black/60"
          />

          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 8 }}
            transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
            className="relative z-10 w-full max-w-md overflow-hidden rounded-3xl border border-[#EAE4DC] bg-white shadow-[0_24px_60px_rgba(24,24,31,0.14)] dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
          >
            <div className="flex items-center justify-between border-b border-[#F2EDE6] px-6 py-4 dark:border-[#2E2D3B]">
              <div className="flex items-center gap-2.5">
                <UserAvatar user={owner} size="sm" />
                <div>
                  <p className="text-xs font-semibold text-[#18181F] dark:text-white">
                    {target.type === 'task'
                      ? 'Task'
                      : target.type === 'reminder'
                      ? 'Reminder'
                      : 'Assignment'}{' '}
                    by {owner?.name}
                  </p>
                  <p className="font-mono text-[11px] text-[#6E6A75]">
                    {formatFullDate(date)} {time ? `· ${formatTime12h(time)}` : ''}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="rounded-full p-2 text-[#6E6A75] hover:bg-[#F5F1EB] dark:hover:bg-[#2C2B38]"
                aria-label="Close detail view"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleSave} className="space-y-4 px-6 py-5">
              <div>
                <label className="mb-1 block text-xs font-medium text-[#6E6A75] dark:text-[#A19CA8]">
                  Title
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-sm font-medium text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-[#6E6A75] dark:text-[#A19CA8]">
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 flex items-center gap-1 text-xs font-medium text-[#6E6A75]">
                    <Calendar className="h-3 w-3" /> Date
                  </label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3.5 py-2 font-mono text-xs text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                  />
                </div>
                <div>
                  <label className="mb-1 flex items-center gap-1 text-xs font-medium text-[#6E6A75]">
                    <Clock className="h-3 w-3" /> Time
                  </label>
                  <input
                    type="time"
                    value={time}
                    onChange={(e) => setTime(e.target.value)}
                    className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3.5 py-2 font-mono text-xs text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-2.5">
                <select
                  aria-label="Priority"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as Priority)}
                  className="rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3 py-2 text-xs font-medium text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                >
                  <option value="low">Low</option>
                  <option value="medium">Medium</option>
                  <option value="high">High</option>
                </select>

                <button
                  type="button"
                  onClick={() =>
                    setVisibility((v) => (v === 'private' ? 'shared' : 'private'))
                  }
                  className="flex items-center justify-center gap-1.5 rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3 py-2 text-xs font-medium text-[#18181F] dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                >
                  {visibility === 'shared' ? (
                    <>
                      <Users className="h-3.5 w-3.5 text-[#9B82F3]" />
                      <span>Shared</span>
                    </>
                  ) : (
                    <>
                      <Lock className="h-3.5 w-3.5 text-[#E684A7]" />
                      <span>My Space</span>
                    </>
                  )}
                </button>

                {target.type === 'assignment' ? (
                  <select
                    aria-label="Status"
                    value={status}
                    onChange={(e) => setStatus(e.target.value as AssignmentStatus)}
                    className="rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3 py-2 text-xs font-medium text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                  >
                    <option value="pending">Pending</option>
                    <option value="in_progress">In progress</option>
                    <option value="completed">Done</option>
                  </select>
                ) : (
                  <button
                    type="button"
                    onClick={() => setCompleted((c) => !c)}
                    className={`rounded-2xl px-3 py-2 text-xs font-semibold transition-colors ${
                      completed
                        ? 'bg-[#DCF4EA] text-[#246A50]'
                        : 'border border-[#EAE4DC] bg-[#FAF7F2] text-[#6E6A75] dark:border-[#343343] dark:bg-[#272733]'
                    }`}
                  >
                    {completed ? 'Completed' : 'Pending'}
                  </button>
                )}
              </div>

              {(target.type === 'task' || target.type === 'assignment') &&
                target.item.attachments.length > 0 && (
                  <div className="space-y-1.5 rounded-2xl bg-[#FAF7F2] p-3 dark:bg-[#272733]">
                    {target.item.attachments.map((att) => (
                      <div
                        key={att.id}
                        className="flex items-center justify-between text-xs text-[#18181F] dark:text-white"
                      >
                        <span className="flex items-center gap-2 truncate">
                          <Paperclip className="h-3.5 w-3.5 text-[#9B82F3]" />
                          <span className="truncate">{att.name}</span>
                        </span>
                        <span className="font-mono text-[11px] text-[#6E6A75]">
                          {formatFileSize(att.sizeBytes)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}

              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={handleDelete}
                  className="inline-flex items-center gap-1.5 rounded-full px-3.5 py-2 text-xs font-medium text-[#DC2626] hover:bg-[#F8D8E4]/50"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  <span>Delete</span>
                </button>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={onClose}
                    className="rounded-full px-4 py-2 text-xs font-semibold text-[#6E6A75] hover:bg-[#F5F1EB] dark:hover:bg-[#2C2B38]"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="rounded-full bg-[#18181F] px-5 py-2 text-xs font-semibold text-white hover:opacity-90 dark:bg-[#9B82F3] dark:text-[#16161D]"
                  >
                    Save
                  </button>
                </div>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
