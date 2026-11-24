import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'motion/react';
import {
  Bell,
  BookOpen,
  CheckSquare,
  Eye,
  FileImage,
  FileText,
  FileUp,
  Paperclip,
  Plus,
  Search,
  StickyNote,
  Trash2,
  Users,
  X,
} from 'lucide-react';
import { AnimatedCheckbox } from '../components/AnimatedCheckbox';
import { EmptyState } from '../components/EmptyState';
import { InspectableItem, ItemDetailModal } from '../components/ItemDetailModal';
import { UserAvatar } from '../components/UserAvatar';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskMutations, useDuoDeskQueries } from '../hooks/useDuoDeskData';
import { AssignmentStatus, AttachmentRef, DocumentItem } from '../types/duodesk';
import {
  detectDocumentKind,
  formatFileSize,
  formatRelativeTime,
  formatShortDate,
  formatTime12h,
  getPriorityMeta,
  isOverdueDate,
} from '../utils/formatters';

type SharedTab = 'tasks' | 'assignments' | 'reminders' | 'notes' | 'documents';
type StatusFilter = 'all' | 'pending' | 'completed';

export function SharedSpacePage() {
  const { isBackendConfigured, setCreateDefaults, pushToast } = useWorkspace();
  const navigate = useNavigate();
  const {
    usersQuery,
    tasksQuery,
    assignmentsQuery,
    remindersQuery,
    notesQuery,
    documentsQuery,
  } = useDuoDeskQueries();
  const mutations = useDuoDeskMutations();

  const [activeTab, setActiveTab] = useState<SharedTab>('tasks');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [search, setSearch] = useState('');
  const [inspectTarget, setInspectTarget] = useState<InspectableItem | null>(null);
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);
  const [newChecklistLine, setNewChecklistLine] = useState('');
  const [previewDoc, setPreviewDoc] = useState<DocumentItem | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const attachInputRef = useRef<HTMLInputElement>(null);
  const [attachingId, setAttachingId] = useState<string | null>(null);

  const users = usersQuery.data;
  const memberList = users ? Object.values(users).slice(0, 2) : [];
  const memberLabel =
    memberList.length >= 2
      ? `${memberList[0].name} & ${memberList[1].name}`
      : memberList[0]?.name ?? 'DuoDesk';
  const q = search.trim().toLowerCase();

  const sharedTasks = useMemo(
    () =>
      (tasksQuery.data ?? [])
        .filter((t) => t.visibility === 'shared')
        .filter((t) =>
          statusFilter === 'all' ? true : statusFilter === 'completed' ? t.completed : !t.completed
        )
        .filter((t) => !q || t.title.toLowerCase().includes(q))
        .sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
    [tasksQuery.data, statusFilter, q]
  );

  const sharedAssignments = useMemo(
    () =>
      (assignmentsQuery.data ?? [])
        .filter((a) => a.visibility === 'shared')
        .filter((a) =>
          statusFilter === 'all'
            ? true
            : statusFilter === 'completed'
            ? a.status === 'completed'
            : a.status !== 'completed'
        )
        .filter((a) => !q || a.title.toLowerCase().includes(q) || a.subject.toLowerCase().includes(q))
        .sort((a, b) => a.deadline.localeCompare(b.deadline)),
    [assignmentsQuery.data, statusFilter, q]
  );

  const sharedReminders = useMemo(
    () =>
      (remindersQuery.data ?? [])
        .filter((r) => r.visibility === 'shared')
        .filter((r) =>
          statusFilter === 'all' ? true : statusFilter === 'completed' ? r.completed : !r.completed
        )
        .filter((r) => !q || r.title.toLowerCase().includes(q))
        .sort((a, b) => a.date.localeCompare(b.date)),
    [remindersQuery.data, statusFilter, q]
  );

  const sharedNotes = useMemo(
    () =>
      (notesQuery.data ?? [])
        .filter((n) => n.visibility === 'shared')
        .filter((n) => !q || n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q))
        .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
    [notesQuery.data, q]
  );

  const sharedDocuments = useMemo(
    () =>
      (documentsQuery.data ?? [])
        .filter((d) => d.visibility === 'shared')
        .filter((d) => !q || d.name.toLowerCase().includes(q)),
    [documentsQuery.data, q]
  );

  useEffect(() => {
    if (
      sharedNotes.length > 0 &&
      (!selectedNoteId || !sharedNotes.some((n) => n.id === selectedNoteId))
    ) {
      setSelectedNoteId(sharedNotes[0].id);
    }
  }, [sharedNotes, selectedNoteId]);

  const activeNote = sharedNotes.find((n) => n.id === selectedNoteId) ?? null;

  const openCreateShared = (tab: SharedTab) => {
    const kind =
      tab === 'assignments'
        ? 'assignment'
        : tab === 'reminders'
        ? 'reminder'
        : tab === 'notes'
        ? 'note'
        : tab === 'documents'
        ? 'document'
        : 'task';
    setCreateDefaults({ kind, visibility: 'shared' });
    navigate('/create');
  };

  const handleUploadSharedFile = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    for (let i = 0; i < files.length; i++) {
      const f = files[i];
      const { kind, extension } = detectDocumentKind(f.name);
      let previewUrl: string | undefined;
      if (f.type.startsWith('image/')) {
        previewUrl = await new Promise<string | undefined>((resolve) => {
          const reader = new FileReader();
          reader.onload = () =>
            resolve(typeof reader.result === 'string' ? reader.result : undefined);
          reader.onerror = () => resolve(undefined);
          reader.readAsDataURL(f);
        });
      }
      await mutations.uploadDocument.mutateAsync({
        name: f.name,
        kind,
        extension,
        mimeType: f.type || 'application/octet-stream',
        sizeBytes: f.size,
        visibility: 'shared',
        previewUrl,
        summary: 'Shared in DuoDesk Shared Space',
        file: f,
      });
    }
  };

  const handleAttachToAssignment = async (files: FileList | null) => {
    if (!files || files.length === 0 || !attachingId) return;
    const target = sharedAssignments.find((a) => a.id === attachingId);
    if (!target) return;
    if (isBackendConfigured) {
      for (let i = 0; i < files.length; i++) {
        await mutations.uploadAttachment.mutateAsync({
          file: files[i],
          visibility: 'shared',
          entityType: 'assignment',
          entityId: target.id,
        });
      }
      setAttachingId(null);
      return;
    }
    const added: AttachmentRef[] = [];
    for (let i = 0; i < files.length; i++) {
      const f = files[i];
      const { kind } = detectDocumentKind(f.name);
      added.push({
        id: `att-${Date.now()}-${i}`,
        name: f.name,
        sizeBytes: f.size,
        kind,
        mimeType: f.type || 'application/octet-stream',
      });
    }
    await mutations.updateAssignment.mutateAsync({
      id: target.id,
      patch: { attachments: [...target.attachments, ...added] },
    });
    pushToast({ title: 'Attachment added', tone: 'success' });
    setAttachingId(null);
  };

  const tabs: { id: SharedTab; label: string; icon: typeof CheckSquare; count: number }[] = [
    { id: 'tasks', label: 'Tasks', icon: CheckSquare, count: sharedTasks.length },
    { id: 'assignments', label: 'Assignments', icon: BookOpen, count: sharedAssignments.length },
    { id: 'reminders', label: 'Reminders', icon: Bell, count: sharedReminders.length },
    { id: 'notes', label: 'Notes', icon: StickyNote, count: sharedNotes.length },
    { id: 'documents', label: 'Documents', icon: FileText, count: sharedDocuments.length },
  ];

  return (
    <div className="space-y-6">
      <input
        ref={attachInputRef}
        type="file"
        multiple
        accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
        className="hidden"
        onChange={(e) => handleAttachToAssignment(e.target.files)}
      />

      {/* Shared Header with Both Avatars */}
      <div className="flex flex-col justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-gradient-to-br from-white via-[#F9F6FE] to-[#E6DFFB]/55 p-6 sm:flex-row sm:items-center dark:border-[#2E2D3B] dark:from-[#1F1F29] dark:to-[#272338]">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            {memberList.length > 0 && (
              <div className="flex -space-x-2">
                {memberList.map((m) => (
                  <UserAvatar key={m.id} user={m} size="sm" />
                ))}
              </div>
            )}
            <span className="text-xs font-semibold text-[#5B469C] dark:text-[#D5C9FA]">
              {memberLabel} · Shared Space
            </span>
          </div>
          <h1 className="font-display text-2xl font-bold text-[#18181F] dark:text-white">
            Shared Space
          </h1>
          <p className="text-xs text-[#6E6A75] dark:text-[#A19CA8]">
            Everything you both collaborate on, cleanly separated from private workspaces.
          </p>
        </div>

        <button
          type="button"
          onClick={() => openCreateShared(activeTab)}
          className="inline-flex shrink-0 items-center gap-1.5 self-start rounded-full bg-[#9B82F3] px-5 py-2.5 text-xs font-semibold text-white hover:opacity-90 sm:self-center dark:text-[#16161D]"
        >
          <Plus className="h-4 w-4" />
          <span>Add shared item</span>
        </button>
      </div>

      {/* Category Pills & Simple Status Filter */}
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div className="flex flex-wrap items-center gap-1.5">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-2 text-xs font-semibold whitespace-nowrap transition-colors ${
                  active
                    ? 'bg-[#E6DFFB] text-[#5B469C] dark:bg-[#2D283E] dark:text-[#E6DFFB]'
                    : 'bg-white text-[#6E6A75] hover:text-[#18181F] dark:bg-[#1F1F29] dark:text-[#A19CA8]'
                }`}
              >
                <Icon className="h-3.5 w-3.5" />
                <span>{tab.label}</span>
                <span className="font-mono text-[11px] opacity-75">{tab.count}</span>
              </button>
            );
          })}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="relative flex-1 sm:w-44">
            <Search className="pointer-events-none absolute top-1/2 left-3.5 h-3.5 w-3.5 -translate-y-1/2 text-[#8E8996]" />
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search..."
              className="w-full rounded-full border border-[#EAE4DC] bg-white py-2 pr-3 pl-9 text-xs text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
            />
          </div>

          {(activeTab === 'tasks' ||
            activeTab === 'assignments' ||
            activeTab === 'reminders') && (
            <div className="flex items-center rounded-full border border-[#EAE4DC] bg-white p-0.5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
              {(['all', 'pending', 'completed'] as const).map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setStatusFilter(s)}
                  className={`rounded-full px-3 py-1.5 text-xs font-semibold capitalize transition-colors ${
                    statusFilter === s
                      ? 'bg-[#18181F] text-white dark:bg-[#9B82F3] dark:text-[#16161D]'
                      : 'text-[#6E6A75]'
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Shared Tasks */}
      {activeTab === 'tasks' && (
        <div className="space-y-3">
          {sharedTasks.length === 0 ? (
            <EmptyState
              icon={Users}
              title="No shared tasks here"
              description="Add a shared task so both Rushil and Alex can track and complete it together."
              actionLabel="Add shared task"
              onAction={() => openCreateShared('tasks')}
            />
          ) : (
            sharedTasks.map((task) => {
              const creator = users?.[task.ownerId];
              const pMeta = getPriorityMeta(task.priority);
              const overdue = isOverdueDate(task.dueDate, task.completed);

              return (
                <div
                  key={task.id}
                  onClick={() => setInspectTarget({ type: 'task', item: task })}
                  className="flex cursor-pointer items-center justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-white px-5 py-4 transition-all hover:-translate-y-0.5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
                >
                  <div className="flex min-w-0 items-center gap-3.5">
                    <AnimatedCheckbox
                      checked={task.completed}
                      accent="lavender"
                      label={`Complete ${task.title}`}
                      onChange={() =>
                        mutations.updateTask.mutate({
                          id: task.id,
                          patch: { completed: !task.completed },
                        })
                      }
                    />
                    <div className="min-w-0">
                      <p
                        className={`truncate text-sm font-semibold ${
                          task.completed ? 'text-[#9A94A3] line-through' : 'text-[#18181F] dark:text-white'
                        }`}
                      >
                        {task.title}
                      </p>
                      <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[#6E6A75] dark:text-[#A19CA8]">
                        <span className="inline-flex items-center gap-1.5 font-medium text-[#18181F] dark:text-white">
                          <UserAvatar user={creator} size="xs" />
                          <span>By {creator?.name}</span>
                        </span>
                        <span aria-hidden="true">·</span>
                        <span className="inline-flex items-center gap-1">
                          <span
                            className="h-2 w-2 rounded-full"
                            style={{ backgroundColor: pMeta.dotColor }}
                          />
                          {pMeta.label}
                        </span>
                        {task.attachments.length > 0 && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span className="inline-flex items-center gap-1">
                              <Paperclip className="h-3 w-3" />
                              {task.attachments.length}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                  <span
                    className={`shrink-0 font-mono text-xs tabular-nums ${
                      overdue ? 'font-semibold text-[#DC2626]' : 'text-[#6E6A75]'
                    }`}
                  >
                    {formatShortDate(task.dueDate)}
                  </span>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Shared Assignments */}
      {activeTab === 'assignments' && (
        <div className="space-y-3">
          {sharedAssignments.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="No shared assignments"
              description="Add a joint project or assignment for you and Alex."
              actionLabel="Add shared assignment"
              onAction={() => openCreateShared('assignments')}
            />
          ) : (
            sharedAssignments.map((asg) => {
              const creator = users?.[asg.ownerId];
              return (
                <div
                  key={asg.id}
                  onClick={() => setInspectTarget({ type: 'assignment', item: asg })}
                  className="flex cursor-pointer flex-wrap items-center justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-white px-5 py-4 transition-all hover:-translate-y-0.5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                      {asg.title}
                    </p>
                    <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[#6E6A75]">
                      <span className="inline-flex items-center gap-1.5">
                        <UserAvatar user={creator} size="xs" />
                        <span>By {creator?.name}</span>
                      </span>
                      <span aria-hidden="true">·</span>
                      <span>{asg.subject}</span>
                      <span aria-hidden="true">·</span>
                      <span className="font-mono">{formatShortDate(asg.deadline)}</span>
                    </div>
                  </div>

                  <div
                    className="flex items-center gap-2.5"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      type="button"
                      onClick={() => {
                        setAttachingId(asg.id);
                        attachInputRef.current?.click();
                      }}
                      className="inline-flex items-center gap-1 rounded-full bg-[#FAF7F2] px-3 py-1.5 text-xs font-medium text-[#6E6A75] hover:text-[#18181F] dark:bg-[#272733]"
                    >
                      <Paperclip className="h-3.5 w-3.5" />
                      <span className="font-mono">{asg.attachments.length}</span>
                    </button>

                    <select
                      aria-label={`Status for ${asg.title}`}
                      value={asg.status}
                      onChange={(e) =>
                        mutations.updateAssignment.mutate({
                          id: asg.id,
                          patch: { status: e.target.value as AssignmentStatus },
                        })
                      }
                      className="rounded-full border border-[#EAE4DC] bg-[#FAF7F2] px-3 py-1.5 text-xs font-semibold text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                    >
                      <option value="pending">Pending</option>
                      <option value="in_progress">In progress</option>
                      <option value="completed">Done</option>
                    </select>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Shared Reminders */}
      {activeTab === 'reminders' && (
        <div className="space-y-3">
          {sharedReminders.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No shared reminders"
              description="Schedule a shared reminder so neither of you misses a sync or submission."
              actionLabel="Add shared reminder"
              onAction={() => openCreateShared('reminders')}
            />
          ) : (
            sharedReminders.map((rem) => {
              const creator = users?.[rem.ownerId];
              return (
                <div
                  key={rem.id}
                  onClick={() => setInspectTarget({ type: 'reminder', item: rem })}
                  className="flex cursor-pointer items-center justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-white px-5 py-4 transition-all hover:-translate-y-0.5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
                >
                  <div className="flex min-w-0 items-center gap-3.5">
                    <AnimatedCheckbox
                      checked={rem.completed}
                      accent="lavender"
                      label={`Complete ${rem.title}`}
                      onChange={() =>
                        mutations.updateReminder.mutate({
                          id: rem.id,
                          patch: { completed: !rem.completed },
                        })
                      }
                    />
                    <div className="min-w-0">
                      <p
                        className={`truncate text-sm font-semibold ${
                          rem.completed ? 'text-[#9A94A3] line-through' : 'text-[#18181F] dark:text-white'
                        }`}
                      >
                        {rem.title}
                      </p>
                      <div className="mt-1 flex items-center gap-2 text-xs text-[#6E6A75]">
                        <UserAvatar user={creator} size="xs" />
                        <span>By {creator?.name}</span>
                        <span aria-hidden="true">·</span>
                        <span className="font-mono">
                          {formatShortDate(rem.date)} · {formatTime12h(rem.time)}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Shared Notes & Checklists */}
      {activeTab === 'notes' && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
          <div className="space-y-2 lg:col-span-4">
            <div className="flex gap-2">
              <button
                type="button"
                onClick={async () => {
                  const created = await mutations.createNote.mutateAsync({
                    title: 'Shared note',
                    content: '',
                    type: 'note',
                    checklist: [],
                    visibility: 'shared',
                  });
                  setSelectedNoteId(created.id);
                }}
                className="flex-1 rounded-full bg-[#9B82F3] py-2 text-xs font-semibold text-white"
              >
                + Note
              </button>
              <button
                type="button"
                onClick={async () => {
                  const created = await mutations.createNote.mutateAsync({
                    title: 'Shared checklist',
                    content: '',
                    type: 'checklist',
                    checklist: [{ id: `c-${Date.now()}`, text: 'First item', checked: false }],
                    visibility: 'shared',
                  });
                  setSelectedNoteId(created.id);
                }}
                className="flex-1 rounded-full border border-[#EAE4DC] bg-white py-2 text-xs font-semibold text-[#18181F] dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
              >
                + Checklist
              </button>
            </div>

            {sharedNotes.map((n) => {
              const creator = users?.[n.ownerId];
              return (
                <button
                  key={n.id}
                  type="button"
                  onClick={() => setSelectedNoteId(n.id)}
                  className={`block w-full rounded-2xl border p-4 text-left transition-all ${
                    n.id === activeNote?.id
                      ? 'border-[#9B82F3] bg-[#E6DFFB]/45 dark:bg-[#2D283E]'
                      : 'border-[#EAE4DC] bg-white hover:bg-[#FAF7F2] dark:border-[#2E2D3B] dark:bg-[#1F1F29]'
                  }`}
                >
                  <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                    {n.title || 'Untitled'}
                  </p>
                  <p className="mt-1 font-mono text-[11px] text-[#6E6A75]">
                    By {creator?.name} · {formatRelativeTime(n.updatedAt)}
                  </p>
                </button>
              );
            })}
          </div>

          <div className="rounded-3xl border border-[#EAE4DC] bg-white p-6 lg:col-span-8 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
            {!activeNote ? (
              <p className="py-16 text-center text-sm text-[#6E6A75]">
                Select or create a shared note.
              </p>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-between gap-3">
                  <input
                    type="text"
                    value={activeNote.title}
                    onChange={(e) =>
                      mutations.updateNote.mutate({
                        id: activeNote.id,
                        patch: { title: e.target.value },
                      })
                    }
                    className="w-full bg-transparent font-display text-xl font-bold text-[#18181F] focus:outline-none dark:text-white"
                  />
                  <button
                    type="button"
                    onClick={() => mutations.deleteNote.mutate(activeNote.id)}
                    className="rounded-full p-2 text-[#8E8996] hover:bg-[#F8D8E4]/60 hover:text-[#DC2626]"
                    aria-label="Delete shared note"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>

                <textarea
                  rows={activeNote.type === 'checklist' ? 3 : 10}
                  value={activeNote.content}
                  onChange={(e) =>
                    mutations.updateNote.mutate({
                      id: activeNote.id,
                      patch: { content: e.target.value },
                    })
                  }
                  placeholder="Write together..."
                  className="w-full resize-none bg-transparent text-sm leading-relaxed text-[#4A4552] focus:outline-none dark:text-[#D1CBD9]"
                />

                {activeNote.type === 'checklist' && (
                  <div className="space-y-2.5 border-t border-[#F2EDE6] pt-4 dark:border-[#2E2D3B]">
                    {activeNote.checklist.map((item) => (
                      <div key={item.id} className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-3">
                          <AnimatedCheckbox
                            checked={item.checked}
                            accent="lavender"
                            label={item.text}
                            onChange={() => {
                              const next = activeNote.checklist.map((c) =>
                                c.id === item.id ? { ...c, checked: !c.checked } : c
                              );
                              mutations.updateNote.mutate({
                                id: activeNote.id,
                                patch: { checklist: next },
                              });
                            }}
                          />
                          <span
                            className={`text-sm ${
                              item.checked ? 'text-[#9A94A3] line-through' : ''
                            }`}
                          >
                            {item.text}
                          </span>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            const next = activeNote.checklist.filter((c) => c.id !== item.id);
                            mutations.updateNote.mutate({
                              id: activeNote.id,
                              patch: { checklist: next },
                            });
                          }}
                          className="text-xs text-[#8E8996] hover:text-[#DC2626]"
                        >
                          Remove
                        </button>
                      </div>
                    ))}
                    <div className="flex gap-2 pt-2">
                      <input
                        type="text"
                        value={newChecklistLine}
                        onChange={(e) => setNewChecklistLine(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' && newChecklistLine.trim()) {
                            e.preventDefault();
                            mutations.updateNote.mutate({
                              id: activeNote.id,
                              patch: {
                                checklist: [
                                  ...activeNote.checklist,
                                  {
                                    id: `c-${Date.now()}`,
                                    text: newChecklistLine.trim(),
                                    checked: false,
                                  },
                                ],
                              },
                            });
                            setNewChecklistLine('');
                          }
                        }}
                        placeholder="Add item..."
                        className="flex-1 rounded-full border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2 text-xs focus:outline-none dark:border-[#343343] dark:bg-[#272733]"
                      />
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Shared Documents */}
      {activeTab === 'documents' && (
        <div className="space-y-4">
          <div
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              handleUploadSharedFile(e.dataTransfer.files);
            }}
            className="flex cursor-pointer flex-col items-center justify-center rounded-3xl border-2 border-dashed border-[#DDD6FE] bg-white py-7 text-center transition-colors hover:border-[#9B82F3] dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              className="hidden"
              onChange={(e) => handleUploadSharedFile(e.target.files)}
            />
            <FileUp className="mb-2 h-5 w-5 text-[#9B82F3]" />
            <p className="text-sm font-semibold text-[#18181F] dark:text-white">
              Drop shared PDFs, Word docs, or images here
            </p>
            <p className="mt-0.5 text-xs text-[#6E6A75]">Visible to {memberLabel}</p>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {sharedDocuments.map((doc) => {
              const owner = users?.[doc.ownerId];
              return (
                <div
                  key={doc.id}
                  className="overflow-hidden rounded-3xl border border-[#EAE4DC] bg-white dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
                >
                  {doc.previewUrl ? (
                    <img
                      src={doc.previewUrl}
                      alt={doc.name}
                      referrerPolicy="no-referrer"
                      onClick={() => setPreviewDoc(doc)}
                      className="h-36 w-full cursor-pointer object-cover"
                    />
                  ) : (
                    <div
                      onClick={() => setPreviewDoc(doc)}
                      className="flex h-24 cursor-pointer items-center justify-center bg-[#FAF7F2] dark:bg-[#272733]"
                    >
                      {doc.kind === 'image' ? (
                        <FileImage className="h-6 w-6 text-[#9B82F3]" />
                      ) : (
                        <FileText className="h-6 w-6 text-[#9B82F3]" />
                      )}
                    </div>
                  )}
                  <div className="p-4">
                    <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                      {doc.name}
                    </p>
                    <p className="mt-1 flex items-center gap-1.5 text-xs text-[#6E6A75]">
                      <UserAvatar user={owner} size="xs" />
                      <span>By {owner?.name}</span>
                      <span aria-hidden="true">·</span>
                      <span className="font-mono">{formatFileSize(doc.sizeBytes)}</span>
                    </p>
                    <div className="mt-3 flex items-center justify-between border-t border-[#F2EDE6] pt-2.5 dark:border-[#2E2D3B]">
                      <button
                        type="button"
                        onClick={() => setPreviewDoc(doc)}
                        className="inline-flex items-center gap-1 text-xs font-medium text-[#6E6A75] hover:text-[#18181F]"
                      >
                        <Eye className="h-3.5 w-3.5" /> Preview
                      </button>
                      <button
                        type="button"
                        onClick={() => mutations.deleteDocument.mutate(doc.id)}
                        className="text-xs font-medium text-[#DC2626] hover:underline"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Preview Modal */}
      <AnimatePresence>
        {previewDoc && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setPreviewDoc(null)}
              className="fixed inset-0 bg-[#18181F]/40 backdrop-blur-xs"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.96 }}
              className="relative z-10 w-full max-w-lg rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
            >
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-display text-base font-bold">{previewDoc.name}</h3>
                <button
                  type="button"
                  onClick={() => setPreviewDoc(null)}
                  className="rounded-full p-1.5 hover:bg-[#FAF7F2]"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              {previewDoc.previewUrl ? (
                <img
                  src={previewDoc.previewUrl}
                  alt={previewDoc.name}
                  referrerPolicy="no-referrer"
                  className="max-h-[55vh] w-full rounded-2xl object-contain"
                />
              ) : (
                <p className="rounded-2xl bg-[#FAF7F2] p-6 text-center text-xs text-[#6E6A75] dark:bg-[#272733]">
                  {previewDoc.summary || 'Shared document stored in DuoDesk.'}
                </p>
              )}
              {previewDoc.downloadUrl && (
                <div className="mt-4 flex justify-end">
                  <a
                    href={previewDoc.downloadUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-full bg-[#9B82F3] px-4 py-2 text-xs font-semibold text-white hover:opacity-90"
                  >
                    Open file
                  </a>
                </div>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {users && (
        <ItemDetailModal
          target={inspectTarget}
          users={users}
          onClose={() => setInspectTarget(null)}
          onSaveTask={async (id, patch) => {
            await mutations.updateTask.mutateAsync({ id, patch });
          }}
          onDeleteTask={async (id) => {
            await mutations.deleteTask.mutateAsync(id);
          }}
          onSaveReminder={async (id, patch) => {
            await mutations.updateReminder.mutateAsync({ id, patch });
          }}
          onDeleteReminder={async (id) => {
            await mutations.deleteReminder.mutateAsync(id);
          }}
          onSaveAssignment={async (id, patch) => {
            await mutations.updateAssignment.mutateAsync({ id, patch });
          }}
          onDeleteAssignment={async (id) => {
            await mutations.deleteAssignment.mutateAsync(id);
          }}
        />
      )}
    </div>
  );
}
