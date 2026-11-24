import React, { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import {
  ArrowLeft,
  Bell,
  BookOpen,
  CheckSquare,
  FileText,
  FileUp,
  Lock,
  Paperclip,
  StickyNote,
  Users,
} from 'lucide-react';
import { CreateModalKind, useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskMutations } from '../hooks/useDuoDeskData';
import {
  AttachmentRef,
  NotificationPreference,
  Priority,
  RepeatOption,
  Visibility,
} from '../types/duodesk';
import { detectDocumentKind, formatFileSize, getTodayIso } from '../utils/formatters';

const KIND_PILLS: { id: CreateModalKind; label: string; icon: typeof CheckSquare }[] = [
  { id: 'task', label: 'Task', icon: CheckSquare },
  { id: 'reminder', label: 'Reminder', icon: Bell },
  { id: 'assignment', label: 'Assignment', icon: BookOpen },
  { id: 'note', label: 'Note', icon: StickyNote },
  { id: 'document', label: 'Document', icon: FileUp },
];

const CATEGORY_PRESETS = ['Study', 'Engineering', 'Academic', 'Personal', 'Design'];

export function CreatePage() {
  const { createDefaults, isBackendConfigured } = useWorkspace();
  const mutations = useDuoDeskMutations();
  const navigate = useNavigate();

  const [kind, setKind] = useState<CreateModalKind>(createDefaults.kind ?? 'task');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState(createDefaults.date ?? getTodayIso());
  const [dueTime, setDueTime] = useState('17:00');
  const [priority, setPriority] = useState<Priority>('medium');
  const [visibility, setVisibility] = useState<Visibility>(
    createDefaults.visibility ?? 'private'
  );
  const [category, setCategory] = useState('Study');
  const [repeat, setRepeat] = useState<RepeatOption>('none');
  const [notifPref, setNotifPref] = useState<NotificationPreference>('push_in_app');
  const [noteMode, setNoteMode] = useState<'note' | 'checklist'>('note');
  const [checklistText, setChecklistText] = useState('First item\nSecond item');
  const [attachments, setAttachments] = useState<AttachmentRef[]>([]);
  const [attachmentFiles, setAttachmentFiles] = useState<Record<string, File>>({});
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [docFile, setDocFile] = useState<{
    name: string;
    sizeBytes: number;
    mimeType: string;
    previewUrl?: string;
    rawFile: File;
  } | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const readPreview = (file: File): Promise<string | undefined> =>
    new Promise((resolve) => {
      if (!file.type.startsWith('image/')) {
        resolve(undefined);
        return;
      }
      const reader = new FileReader();
      reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : undefined);
      reader.onerror = () => resolve(undefined);
      reader.readAsDataURL(file);
    });

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    if (kind === 'document') {
      const f = files[0];
      const previewUrl = await readPreview(f);
      setUploadProgress(25);
      window.setTimeout(() => {
        setUploadProgress(100);
        setDocFile({
          name: f.name,
          sizeBytes: f.size,
          mimeType: f.type || 'application/octet-stream',
          previewUrl,
          rawFile: f,
        });
        if (!title) setTitle(f.name);
      }, 220);
      return;
    }

    const added: AttachmentRef[] = [];
    const addedFiles: Record<string, File> = {};
    for (let i = 0; i < files.length; i++) {
      const f = files[i];
      const { kind: docKind } = detectDocumentKind(f.name);
      const previewUrl = await readPreview(f);
      const attId = `att-${Date.now()}-${i}`;
      added.push({
        id: attId,
        name: f.name,
        sizeBytes: f.size,
        kind: docKind,
        mimeType: f.type || 'application/octet-stream',
        previewUrl,
      });
      addedFiles[attId] = f;
    }
    setAttachments((prev) => [...prev, ...added]);
    setAttachmentFiles((prev) => ({ ...prev, ...addedFiles }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (kind !== 'document' && !title.trim()) return;

    if (kind === 'task') {
      const created = await mutations.createTask.mutateAsync({
        title: title.trim(),
        description: description.trim() || undefined,
        visibility,
        priority,
        dueDate,
        dueTime: dueTime || undefined,
        completed: false,
        category: category.trim() || 'General',
        attachments: isBackendConfigured ? [] : attachments,
      });
      if (isBackendConfigured && attachments.length > 0) {
        for (const att of attachments) {
          const raw = attachmentFiles[att.id];
          if (raw) {
            await mutations.uploadAttachment.mutateAsync({
              file: raw,
              visibility,
              entityType: 'task',
              entityId: created.id,
            });
          }
        }
      }
    } else if (kind === 'reminder') {
      await mutations.createReminder.mutateAsync({
        title: title.trim(),
        description: description.trim(),
        visibility,
        date: dueDate,
        time: dueTime || '09:00',
        priority,
        repeat,
        notificationPref: notifPref,
        completed: false,
      });
    } else if (kind === 'assignment') {
      const created = await mutations.createAssignment.mutateAsync({
        title: title.trim(),
        subject: category.trim() || 'General',
        description: description.trim(),
        visibility,
        deadline: dueDate,
        deadlineTime: dueTime || '23:59',
        priority,
        status: 'pending',
        attachments: isBackendConfigured ? [] : attachments,
      });
      if (isBackendConfigured && attachments.length > 0) {
        for (const att of attachments) {
          const raw = attachmentFiles[att.id];
          if (raw) {
            await mutations.uploadAttachment.mutateAsync({
              file: raw,
              visibility,
              entityType: 'assignment',
              entityId: created.id,
            });
          }
        }
      }
    } else if (kind === 'note') {
      const checklistRows =
        noteMode === 'checklist'
          ? checklistText
              .split('\n')
              .map((l) => l.trim())
              .filter(Boolean)
              .map((text, idx) => ({
                id: `chk-${Date.now()}-${idx}`,
                text,
                checked: false,
              }))
          : [];
      await mutations.createNote.mutateAsync({
        title: title.trim(),
        content: description.trim(),
        type: noteMode,
        checklist: checklistRows,
        visibility,
      });
    } else if (kind === 'document') {
      const fileObj = docFile ?? {
        name: title.trim() || 'Shared_Document.pdf',
        sizeBytes: 380000,
        mimeType: 'application/pdf',
        previewUrl: undefined,
        rawFile: undefined,
      };
      const { kind: docKind, extension } = detectDocumentKind(fileObj.name);
      await mutations.uploadDocument.mutateAsync({
        name: title.trim() || fileObj.name,
        kind: docKind,
        extension,
        mimeType: fileObj.mimeType,
        sizeBytes: fileObj.sizeBytes,
        visibility,
        previewUrl: fileObj.previewUrl,
        summary: description.trim() || undefined,
        file: fileObj.rawFile,
      });
    }

    navigate(visibility === 'shared' ? '/shared-space' : '/my-space');
  };

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 rounded-full border border-[#EAE4DC] bg-white px-3.5 py-2 text-xs font-semibold text-[#18181F] hover:bg-[#F5F1EB] dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          <span>Back</span>
        </button>

        {/* Kind Selector Pills */}
        <div className="flex flex-wrap items-center gap-1 rounded-full border border-[#EAE4DC] bg-white p-1 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
          {KIND_PILLS.map((p) => {
            const Icon = p.icon;
            const active = kind === p.id;
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => setKind(p.id)}
                className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-colors ${
                  active
                    ? 'bg-[#18181F] text-white dark:bg-[#9B82F3] dark:text-[#16161D]'
                    : 'text-[#6E6A75] hover:text-[#18181F] dark:text-[#A19CA8]'
                }`}
              >
                <Icon className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">{p.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Create Card */}
      <motion.form
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
        onSubmit={handleSubmit}
        className="space-y-5 rounded-3xl border border-[#EAE4DC] bg-white p-6 shadow-[0_12px_36px_rgba(24,24,31,0.04)] sm:p-8 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
      >
        <div>
          <h1 className="font-display text-2xl font-bold tracking-tight text-[#18181F] dark:text-white">
            {kind === 'task'
              ? 'Create Task'
              : kind === 'reminder'
              ? 'Create Reminder'
              : kind === 'assignment'
              ? 'Create Assignment'
              : kind === 'note'
              ? 'Create Note'
              : 'Upload Document'}
          </h1>
          <p className="mt-1 text-xs text-[#6E6A75] dark:text-[#A19CA8]">
            Choose My Space to keep it private or Shared Space to collaborate.
          </p>
        </div>

        {/* Workspace Selection */}
        <div>
          <label className="mb-2 block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
            Workspace
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setVisibility('private')}
              className={`flex items-center justify-center gap-2 rounded-full border py-2.5 text-xs font-semibold transition-all ${
                visibility === 'private'
                  ? 'border-[#E684A7] bg-[#F8D8E4]/65 text-[#8E3B5B] dark:bg-[#3A2632] dark:text-[#F8D8E4]'
                  : 'border-[#EAE4DC] bg-[#FAF7F2] text-[#6E6A75] dark:border-[#343343] dark:bg-[#272733]'
              }`}
            >
              <Lock className="h-3.5 w-3.5" />
              <span>My Space</span>
            </button>

            <button
              type="button"
              onClick={() => setVisibility('shared')}
              className={`flex items-center justify-center gap-2 rounded-full border py-2.5 text-xs font-semibold transition-all ${
                visibility === 'shared'
                  ? 'border-[#9B82F3] bg-[#E6DFFB]/80 text-[#5B469C] dark:bg-[#2D283E] dark:text-[#E6DFFB]'
                  : 'border-[#EAE4DC] bg-[#FAF7F2] text-[#6E6A75] dark:border-[#343343] dark:bg-[#272733]'
              }`}
            >
              <Users className="h-3.5 w-3.5" />
              <span>Shared Space</span>
            </button>
          </div>
        </div>

        {kind === 'document' ? (
          <div className="space-y-4">
            <div
              onClick={() => fileRef.current?.click()}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                handleFiles(e.dataTransfer.files);
              }}
              className="flex cursor-pointer flex-col items-center justify-center rounded-3xl border-2 border-dashed border-[#DDD6FE] bg-[#FAF7F2] px-6 py-8 text-center transition-colors hover:border-[#9B82F3] dark:border-[#3A384A] dark:bg-[#272733]"
            >
              <FileUp className="mb-2 h-6 w-6 text-[#9B82F3]" />
              <p className="text-sm font-semibold text-[#18181F] dark:text-white">
                {docFile ? docFile.name : 'Choose PDF, Word doc, or image'}
              </p>
              <p className="mt-1 text-xs text-[#6E6A75]">
                Drag and drop or click to browse
              </p>
              {uploadProgress !== null && (
                <div className="mt-3 w-full max-w-xs">
                  <div className="h-1.5 overflow-hidden rounded-full bg-[#EAE4DC]">
                    <div
                      className="h-full bg-[#9B82F3] transition-all"
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                  <p className="mt-1 font-mono text-[11px] text-[#6E6A75]">
                    {docFile ? formatFileSize(docFile.sizeBytes) : `${uploadProgress}%`}
                  </p>
                </div>
              )}
            </div>
            <input
              ref={fileRef}
              type="file"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              className="hidden"
              onChange={(e) => handleFiles(e.target.files)}
            />
            <div>
              <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75]">
                File title
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g., DBMS_Assignment.pdf"
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-3 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>
            <div>
              <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75]">
                Short note (optional)
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Add context for this document..."
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-3 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>
          </div>
        ) : (
          <>
            {/* Title */}
            <div>
              <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
                {kind === 'task' ? 'Task title' : 'Title'}
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={
                  kind === 'task'
                    ? 'e.g., Complete Python project'
                    : kind === 'reminder'
                    ? 'e.g., Sync with Alex at 3 PM'
                    : kind === 'assignment'
                    ? 'e.g., Submit DBMS assignment'
                    : 'e.g., Weekly reading notes'
                }
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-3 text-sm font-medium text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>

            {kind === 'note' && (
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setNoteMode('note')}
                  className={`rounded-full px-3.5 py-1.5 text-xs font-semibold ${
                    noteMode === 'note'
                      ? 'bg-[#E6DFFB] text-[#5B469C]'
                      : 'bg-[#FAF7F2] text-[#6E6A75] dark:bg-[#272733]'
                  }`}
                >
                  Note
                </button>
                <button
                  type="button"
                  onClick={() => setNoteMode('checklist')}
                  className={`rounded-full px-3.5 py-1.5 text-xs font-semibold ${
                    noteMode === 'checklist'
                      ? 'bg-[#E6DFFB] text-[#5B469C]'
                      : 'bg-[#FAF7F2] text-[#6E6A75] dark:bg-[#272733]'
                  }`}
                >
                  Checklist
                </button>
              </div>
            )}

            {/* Description */}
            <div>
              <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
                Description
              </label>
              <textarea
                rows={kind === 'note' && noteMode === 'note' ? 5 : 3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Add helpful context or details..."
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-3 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>

            {kind === 'note' && noteMode === 'checklist' && (
              <div>
                <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75]">
                  Checklist items (one per line)
                </label>
                <textarea
                  rows={4}
                  value={checklistText}
                  onChange={(e) => setChecklistText(e.target.value)}
                  className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-3 font-mono text-xs text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                />
              </div>
            )}

            {kind !== 'note' && (
              <>
                {/* Due Date & Due Time */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
                      Due date
                    </label>
                    <input
                      type="date"
                      value={dueDate}
                      onChange={(e) => setDueDate(e.target.value)}
                      className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 font-mono text-xs text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                    />
                  </div>
                  <div>
                    <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
                      Due time
                    </label>
                    <input
                      type="time"
                      value={dueTime}
                      onChange={(e) => setDueTime(e.target.value)}
                      className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 font-mono text-xs text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                    />
                  </div>
                </div>

                {/* Priority Pills */}
                <div>
                  <label className="mb-2 block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
                    Priority
                  </label>
                  <div className="grid grid-cols-3 gap-2.5">
                    {(
                      [
                        {
                          id: 'low',
                          label: 'Low',
                          activeClass: 'bg-[#DCF4EA] text-[#246A50] border-[#5BB992]',
                        },
                        {
                          id: 'medium',
                          label: 'Medium',
                          activeClass: 'bg-[#FCE6D4] text-[#9A5528] border-[#F2A36B]',
                        },
                        {
                          id: 'high',
                          label: 'High',
                          activeClass: 'bg-[#F8D8E4] text-[#8E3B5B] border-[#E684A7]',
                        },
                      ] as const
                    ).map((opt) => (
                      <button
                        key={opt.id}
                        type="button"
                        onClick={() => setPriority(opt.id)}
                        className={`rounded-full border py-2 text-xs font-semibold transition-all ${
                          priority === opt.id
                            ? opt.activeClass
                            : 'border-[#EAE4DC] bg-[#FAF7F2] text-[#6E6A75] dark:border-[#343343] dark:bg-[#272733]'
                        }`}
                      >
                        {opt.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Reminder Extras */}
                {kind === 'reminder' && (
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75]">
                        Repeat
                      </label>
                      <select
                        value={repeat}
                        onChange={(e) => setRepeat(e.target.value as RepeatOption)}
                        className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3.5 py-2.5 text-xs text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                      >
                        <option value="none">No repeat</option>
                        <option value="daily">Daily</option>
                        <option value="weekly">Weekly</option>
                        <option value="monthly">Monthly</option>
                      </select>
                    </div>
                    <div>
                      <label className="mb-1.5 block text-xs font-semibold text-[#6E6A75]">
                        Alert
                      </label>
                      <select
                        value={notifPref}
                        onChange={(e) =>
                          setNotifPref(e.target.value as NotificationPreference)
                        }
                        className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-3.5 py-2.5 text-xs text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                      >
                        <option value="push_in_app">Push &amp; In-App</option>
                        <option value="in_app">In-App only</option>
                        <option value="email_digest">Email digest</option>
                      </select>
                    </div>
                  </div>
                )}

                {/* Optional Project / Category */}
                {(kind === 'task' || kind === 'assignment') && (
                  <div className="space-y-2.5">
                    <label className="block text-xs font-semibold text-[#6E6A75] dark:text-[#A19CA8]">
                      {kind === 'assignment' ? 'Subject' : 'Category (optional)'}
                    </label>
                    <div className="flex flex-wrap gap-1.5">
                      {CATEGORY_PRESETS.map((preset) => (
                        <button
                          key={preset}
                          type="button"
                          onClick={() => setCategory(preset)}
                          className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                            category === preset
                              ? 'bg-[#E6DFFB] text-[#5B469C]'
                              : 'bg-[#FAF7F2] text-[#6E6A75] hover:bg-[#F2EDE6] dark:bg-[#272733]'
                          }`}
                        >
                          {preset}
                        </button>
                      ))}
                    </div>
                    <input
                      type="text"
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      placeholder="Or enter a custom category..."
                      className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-xs text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                    />
                  </div>
                )}

                {/* Optional Attachment */}
                {(kind === 'task' || kind === 'assignment') && (
                  <div>
                    <input
                      ref={fileRef}
                      type="file"
                      multiple
                      accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
                      className="hidden"
                      onChange={(e) => handleFiles(e.target.files)}
                    />
                    <button
                      type="button"
                      onClick={() => fileRef.current?.click()}
                      className="inline-flex items-center gap-2 rounded-full border border-dashed border-[#D9D1C7] px-4 py-2 text-xs font-medium text-[#6E6A75] hover:border-[#9B82F3] hover:text-[#18181F] dark:border-[#3A384A] dark:text-[#A19CA8]"
                    >
                      <Paperclip className="h-3.5 w-3.5" />
                      <span>Attach PDF, Word doc, or image</span>
                    </button>
                    {attachments.length > 0 && (
                      <div className="mt-2 space-y-1">
                        {attachments.map((att) => (
                          <div
                            key={att.id}
                            className="flex items-center justify-between rounded-xl bg-[#FAF7F2] px-3 py-1.5 text-xs dark:bg-[#272733]"
                          >
                            <span className="flex items-center gap-2 truncate">
                              <FileText className="h-3.5 w-3.5 text-[#9B82F3]" />
                              <span className="truncate">{att.name}</span>
                            </span>
                            <button
                              type="button"
                              onClick={() =>
                                setAttachments((prev) =>
                                  prev.filter((a) => a.id !== att.id)
                                )
                              }
                              className="text-[#DC2626] hover:underline"
                            >
                              Remove
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </>
        )}

        <button
          type="submit"
          className="w-full rounded-full bg-[#18181F] py-3.5 text-sm font-semibold text-white shadow-sm transition-transform duration-150 hover:-translate-y-0.5 active:translate-y-0 dark:bg-[#9B82F3] dark:text-[#16161D]"
        >
          {kind === 'task'
            ? 'Save Task'
            : kind === 'reminder'
            ? 'Save Reminder'
            : kind === 'assignment'
            ? 'Save Assignment'
            : kind === 'note'
            ? 'Save Note'
            : 'Upload File'}
        </button>
      </motion.form>
    </div>
  );
}
