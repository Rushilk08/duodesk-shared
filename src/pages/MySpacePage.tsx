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
  Lock,
  Paperclip,
  Plus,
  Search,
  StickyNote,
  Trash2,
  X,
} from 'lucide-react';
import { AnimatedCheckbox } from '../components/AnimatedCheckbox';
import { EmptyState } from '../components/EmptyState';
import { InspectableItem, ItemDetailModal } from '../components/ItemDetailModal';
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

type SpaceTab = 'tasks' | 'reminders' | 'assignments' | 'notes' | 'documents';

export function MySpacePage() {
  const { currentUserId, isBackendConfigured, setCreateDefaults, pushToast } = useWorkspace();
  const navigate = useNavigate();
  const {
    usersQuery,
    tasksQuery,
    remindersQuery,
    assignmentsQuery,
    notesQuery,
    documentsQuery,
  } = useDuoDeskQueries();
  const mutations = useDuoDeskMutations();

  const [activeTab, setActiveTab] = useState<SpaceTab>('tasks');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'pending' | 'completed'>('all');
  const [inspectTarget, setInspectTarget] = useState<InspectableItem | null>(null);
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);
  const [newChecklistLine, setNewChecklistLine] = useState('');
  const [previewDoc, setPreviewDoc] = useState<DocumentItem | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const attachInputRef = useRef<HTMLInputElement>(null);
  const [attachingAssignmentId, setAttachingAssignmentId] = useState<string | null>(null);

  const users = usersQuery.data;
  const currentUser = currentUserId && users ? users[currentUserId] : undefined;
  const q = search.trim().toLowerCase();

  const tasks = useMemo(
    () =>
      (tasksQuery.data ?? [])
        .filter((t) => t.visibility === 'private' && t.ownerId === currentUserId)
        .filter((t) =>
          statusFilter === 'all' ? true : statusFilter === 'completed' ? t.completed : !t.completed
        )
        .filter((t) => !q || t.title.toLowerCase().includes(q))
        .sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
    [tasksQuery.data, currentUserId, statusFilter, q]
  );

  const reminders = useMemo(
    () =>
      (remindersQuery.data ?? [])
        .filter((r) => r.visibility === 'private' && r.ownerId === currentUserId)
        .filter((r) =>
          statusFilter === 'all' ? true : statusFilter === 'completed' ? r.completed : !r.completed
        )
        .filter((r) => !q || r.title.toLowerCase().includes(q))
        .sort((a, b) => a.date.localeCompare(b.date)),
    [remindersQuery.data, currentUserId, statusFilter, q]
  );

  const assignments = useMemo(
    () =>
      (assignmentsQuery.data ?? [])
        .filter((a) => a.visibility === 'private' && a.ownerId === currentUserId)
        .filter((a) =>
          statusFilter === 'all'
            ? true
            : statusFilter === 'completed'
            ? a.status === 'completed'
            : a.status !== 'completed'
        )
        .filter((a) => !q || a.title.toLowerCase().includes(q) || a.subject.toLowerCase().includes(q))
        .sort((a, b) => a.deadline.localeCompare(b.deadline)),
    [assignmentsQuery.data, currentUserId, statusFilter, q]
  );

  const notes = useMemo(
    () =>
      (notesQuery.data ?? [])
        .filter((n) => n.visibility === 'private' && n.ownerId === currentUserId)
        .filter((n) => !q || n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q))
        .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
    [notesQuery.data, currentUserId, q]
  );

  const documents = useMemo(
    () =>
      (documentsQuery.data ?? [])
        .filter((d) => d.visibility === 'private' && d.ownerId === currentUserId)
        .filter((d) => !q || d.name.toLowerCase().includes(q)),
    [documentsQuery.data, currentUserId, q]
  );

  useEffect(() => {
    if (notes.length > 0 && (!selectedNoteId || !notes.some((n) => n.id === selectedNoteId))) {
      setSelectedNoteId(notes[0].id);
    }
  }, [notes, selectedNoteId]);

  const activeNote = notes.find((n) => n.id === selectedNoteId) ?? null;

  const openCreateForTab = (tab: SpaceTab) => {
    const kind =
      tab === 'reminders'
        ? 'reminder'
        : tab === 'assignments'
        ? 'assignment'
        : tab === 'notes'
        ? 'note'
        : tab === 'documents'
        ? 'document'
        : 'task';
    setCreateDefaults({ kind, visibility: 'private' });
    navigate('/create');
  };

  const handleUploadPrivateFile = async (files: FileList | null) => {
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
        visibility: 'private',
        previewUrl,
        summary: 'Saved in My Space',
        file: f,
      });
    }
  };

  const handleAttachToAssignment = async (files: FileList | null) => {
    if (!files || files.length === 0 || !attachingAssignmentId) return;
    const target = assignments.find((a) => a.id === attachingAssignmentId);
    if (!target) return;
    if (isBackendConfigured) {
      for (let i = 0; i < files.length; i++) {
        await mutations.uploadAttachment.mutateAsync({
          file: files[i],
          visibility: 'private',
          entityType: 'assignment',
          entityId: target.id,
        });
      }
      setAttachingAssignmentId(null);
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
    setAttachingAssignmentId(null);
  };

  const tabs: { id: SpaceTab; label: string; icon: typeof CheckSquare; count: number }[] = [
    { id: 'tasks', label: 'Tasks', icon: CheckSquare, count: tasks.length },
    { id: 'reminders', label: 'Reminders', icon: Bell, count: reminders.length },
    { id: 'assignments', label: 'Assignments', icon: BookOpen, count: assignments.length },
    { id: 'notes', label: 'Notes', icon: StickyNote, count: notes.length },
    { id: 'documents', label: 'Documents', icon: FileText, count: documents.length },
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

      {/* Header Card */}
      <div className="flex flex-col justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-white p-6 sm:flex-row sm:items-center dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
        <div>
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#8E3B5B] dark:text-[#F8D8E4]">
            <Lock className="h-3.5 w-3.5" />
            Private to {currentUser?.name}
          </span>
          <h1 className="mt-1 font-display text-2xl font-bold text-[#18181F] dark:text-white">
            My Space
          </h1>
          <p className="mt-0.5 text-xs text-[#6E6A75] dark:text-[#A19CA8]">
            Personal tasks, reminders, assignments, notes, and private files.
          </p>
        </div>

        <button
          type="button"
          onClick={() => openCreateForTab(activeTab)}
          className="inline-flex shrink-0 items-center gap-1.5 self-start rounded-full bg-[#18181F] px-5 py-2.5 text-xs font-semibold text-white hover:opacity-90 sm:self-center dark:bg-[#9B82F3] dark:text-[#16161D]"
        >
          <Plus className="h-4 w-4" />
          <span>Add to My Space</span>
        </button>
      </div>

      {/* Search & Tabs */}
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
                    ? 'bg-[#F8D8E4] text-[#8E3B5B] dark:bg-[#2D283E] dark:text-[#F8D8E4]'
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

        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-52">
            <Search className="pointer-events-none absolute top-1/2 left-3.5 h-3.5 w-3.5 -translate-y-1/2 text-[#8E8996]" />
            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search My Space..."
              className="w-full rounded-full border border-[#EAE4DC] bg-white py-2 pr-3 pl-9 text-xs text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
            />
          </div>

          {(activeTab === 'tasks' || activeTab === 'reminders' || activeTab === 'assignments') && (
            <select
              aria-label="Filter status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as 'all' | 'pending' | 'completed')}
              className="rounded-full border border-[#EAE4DC] bg-white px-3 py-2 text-xs font-medium text-[#18181F] focus:outline-none dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
            >
              <option value="all">All</option>
              <option value="pending">Pending</option>
              <option value="completed">Done</option>
            </select>
          )}
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'tasks' && (
        <div className="space-y-3">
          {tasks.length === 0 ? (
            <EmptyState
              icon={CheckSquare}
              title="No personal tasks yet"
              description="Create a private task to organize your day."
              actionLabel="Create task"
              onAction={() => openCreateForTab('tasks')}
            />
          ) : (
            tasks.map((task) => {
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
                      accent="pink"
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
                      <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-[#6E6A75]">
                        <span className="inline-flex items-center gap-1">
                          <span
                            className="h-2 w-2 rounded-full"
                            style={{ backgroundColor: pMeta.dotColor }}
                          />
                          {pMeta.label}
                        </span>
                        {task.category && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span>{task.category}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span
                      className={`font-mono text-xs tabular-nums ${
                        overdue ? 'font-semibold text-[#DC2626]' : 'text-[#6E6A75]'
                      }`}
                    >
                      {formatShortDate(task.dueDate)}
                    </span>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        mutations.deleteTask.mutate(task.id);
                      }}
                      className="rounded-full p-1.5 text-[#8E8996] hover:bg-[#F8D8E4]/60 hover:text-[#DC2626]"
                      aria-label={`Delete ${task.title}`}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {activeTab === 'reminders' && (
        <div className="space-y-3">
          {reminders.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No private reminders"
              description="Schedule a personal reminder with custom repeat and alert settings."
              actionLabel="Add reminder"
              onAction={() => openCreateForTab('reminders')}
            />
          ) : (
            reminders.map((rem) => (
              <div
                key={rem.id}
                onClick={() => setInspectTarget({ type: 'reminder', item: rem })}
                className="flex cursor-pointer items-center justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-white px-5 py-4 transition-all hover:-translate-y-0.5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
              >
                <div className="flex min-w-0 items-center gap-3.5">
                  <AnimatedCheckbox
                    checked={rem.completed}
                    accent="peach"
                    label={`Toggle ${rem.title}`}
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
                    <p className="mt-0.5 font-mono text-xs text-[#6E6A75]">
                      {formatShortDate(rem.date)} · {formatTime12h(rem.time)}
                      {rem.repeat !== 'none' ? ` · ${rem.repeat}` : ''}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    mutations.deleteReminder.mutate(rem.id);
                  }}
                  className="rounded-full p-1.5 text-[#8E8996] hover:bg-[#F8D8E4]/60 hover:text-[#DC2626]"
                  aria-label={`Delete ${rem.title}`}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === 'assignments' && (
        <div className="space-y-3">
          {assignments.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="No private assignments"
              description="Track coursework deadlines and attach PDFs or documents privately."
              actionLabel="Add assignment"
              onAction={() => openCreateForTab('assignments')}
            />
          ) : (
            assignments.map((asg) => (
              <div
                key={asg.id}
                onClick={() => setInspectTarget({ type: 'assignment', item: asg })}
                className="flex cursor-pointer flex-wrap items-center justify-between gap-4 rounded-3xl border border-[#EAE4DC] bg-white px-5 py-4 transition-all hover:-translate-y-0.5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                    {asg.title}
                  </p>
                  <p className="mt-0.5 text-xs text-[#6E6A75]">
                    {asg.subject} · Due {formatShortDate(asg.deadline)}
                  </p>
                </div>

                <div
                  className="flex items-center gap-2.5"
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setAttachingAssignmentId(asg.id);
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

                  <button
                    type="button"
                    onClick={() => mutations.deleteAssignment.mutate(asg.id)}
                    className="rounded-full p-1.5 text-[#8E8996] hover:bg-[#F8D8E4]/60 hover:text-[#DC2626]"
                    aria-label={`Delete ${asg.title}`}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === 'notes' && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
          <div className="space-y-2 lg:col-span-4">
            <div className="flex gap-2">
              <button
                type="button"
                onClick={async () => {
                  const created = await mutations.createNote.mutateAsync({
                    title: 'Untitled note',
                    content: '',
                    type: 'note',
                    checklist: [],
                    visibility: 'private',
                  });
                  setSelectedNoteId(created.id);
                }}
                className="flex-1 rounded-full bg-[#18181F] py-2 text-xs font-semibold text-white dark:bg-[#9B82F3] dark:text-[#16161D]"
              >
                + Note
              </button>
              <button
                type="button"
                onClick={async () => {
                  const created = await mutations.createNote.mutateAsync({
                    title: 'New checklist',
                    content: '',
                    type: 'checklist',
                    checklist: [{ id: `c-${Date.now()}`, text: 'First item', checked: false }],
                    visibility: 'private',
                  });
                  setSelectedNoteId(created.id);
                }}
                className="flex-1 rounded-full border border-[#EAE4DC] bg-white py-2 text-xs font-semibold text-[#18181F] dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
              >
                + Checklist
              </button>
            </div>

            {notes.map((n) => (
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
                  {formatRelativeTime(n.updatedAt)}
                </p>
              </button>
            ))}
          </div>

          <div className="rounded-3xl border border-[#EAE4DC] bg-white p-6 lg:col-span-8 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
            {!activeNote ? (
              <p className="py-16 text-center text-sm text-[#6E6A75]">
                Select or create a private note.
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
                    aria-label="Delete note"
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
                  placeholder="Write your thoughts..."
                  className="w-full resize-none bg-transparent text-sm leading-relaxed text-[#4A4552] focus:outline-none dark:text-[#D1CBD9]"
                />

                {activeNote.type === 'checklist' && (
                  <div className="space-y-2.5 border-t border-[#F2EDE6] pt-4 dark:border-[#2E2D3B]">
                    {activeNote.checklist.map((item) => (
                      <div key={item.id} className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-3">
                          <AnimatedCheckbox
                            checked={item.checked}
                            accent="sage"
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

      {activeTab === 'documents' && (
        <div className="space-y-4">
          <div
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              handleUploadPrivateFile(e.dataTransfer.files);
            }}
            className="flex cursor-pointer flex-col items-center justify-center rounded-3xl border-2 border-dashed border-[#EAE4DC] bg-white py-7 text-center transition-colors hover:border-[#E684A7] dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              className="hidden"
              onChange={(e) => handleUploadPrivateFile(e.target.files)}
            />
            <FileUp className="mb-2 h-5 w-5 text-[#E684A7]" />
            <p className="text-sm font-semibold text-[#18181F] dark:text-white">
              Drop private PDFs, Word docs, or images here
            </p>
            <p className="mt-0.5 text-xs text-[#6E6A75]">Only visible to {currentUser?.name}</p>
          </div>

          {documents.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No private documents"
              description="Upload personal study sheets, PDFs, or images here."
            />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {documents.map((doc) => (
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
                        <FileText className="h-6 w-6 text-[#E684A7]" />
                      )}
                    </div>
                  )}
                  <div className="p-4">
                    <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                      {doc.name}
                    </p>
                    <p className="mt-0.5 font-mono text-xs text-[#6E6A75]">
                      {doc.extension} · {formatFileSize(doc.sizeBytes)}
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
              ))}
            </div>
          )}
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
                  {previewDoc.summary || 'Private document stored in My Space.'}
                </p>
              )}
              {previewDoc.downloadUrl && (
                <div className="mt-4 flex justify-end">
                  <a
                    href={previewDoc.downloadUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-full bg-[#18181F] px-4 py-2 text-xs font-semibold text-white hover:opacity-90 dark:bg-[#9B82F3] dark:text-[#16161D]"
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
