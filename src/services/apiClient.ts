import {
  ActivityEvent,
  AssignmentItem,
  AuthSession,
  DocumentItem,
  InviteInfo,
  NoteItem,
  NotificationItem,
  NotificationSettings,
  ReminderItem,
  TaskItem,
  UserId,
  UserProfile,
  Visibility,
  WorkspaceInfo,
} from '../types/duodesk';
import {
  isVisibleToUser,
  loadSnapshot,
  resetSnapshot,
  saveSnapshot,
} from './mockRepository';

const RAW_API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || '';
const API_BASE_URL = RAW_API_BASE.replace(/\/+$/, '');

// Keep JWT access token strictly in JS memory (never localStorage/sessionStorage)
let inMemoryAccessToken: string | null = null;

export function setApiAccessToken(token: string | null): void {
  inMemoryAccessToken = token;
}

export function getApiAccessToken(): string | null {
  return inMemoryAccessToken;
}

function uid(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`;
}

async function simulateLatency(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 110));
}

async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (inMemoryAccessToken) {
    headers.set('Authorization', `Bearer ${inMemoryAccessToken}`);
  }
  if (!(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const data = await res.json();
      if (typeof data?.detail === 'string') {
        message = data.detail;
      } else if (Array.isArray(data?.detail) && data.detail[0]?.msg) {
        message = data.detail[0].msg;
      }
    } catch {
      // ignore non-JSON error body
    }
    throw new Error(message);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export type DocumentUploadInput = Omit<DocumentItem, 'id' | 'ownerId' | 'uploadedAt'> & {
  file?: File;
};

export const duodeskApi = {
  isBackendConfigured: Boolean(API_BASE_URL),
  isMockMode: !API_BASE_URL,

  // -------------------------------------------------------------------------
  // AUTHENTICATION & WORKSPACE
  // -------------------------------------------------------------------------
  async register(payload: {
    email: string;
    password: string;
    name: string;
    fullName?: string;
    role?: string;
    inviteCode?: string;
  }): Promise<AuthSession> {
    if (!API_BASE_URL) {
      throw new Error('Backend API URL is not configured.');
    }
    const session = await apiFetch<AuthSession>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        email: payload.email,
        password: payload.password,
        name: payload.name,
        full_name: payload.fullName || payload.name,
        role: payload.role || 'Member',
        invite_code: payload.inviteCode?.trim() || undefined,
      }),
    });
    if (session.accessToken) {
      setApiAccessToken(session.accessToken);
    }
    return session;
  },

  async login(email: string, password: string): Promise<AuthSession> {
    if (!API_BASE_URL) {
      throw new Error('Backend API URL is not configured.');
    }
    const session = await apiFetch<AuthSession>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    setApiAccessToken(session.accessToken);
    return session;
  },

  async logout(): Promise<void> {
    if (API_BASE_URL && inMemoryAccessToken) {
      try {
        await apiFetch('/api/auth/logout', { method: 'POST' });
      } catch {
        // clear token locally regardless
      }
    }
    setApiAccessToken(null);
  },

  async requestPasswordReset(email: string): Promise<void> {
    if (!API_BASE_URL) return;
    await apiFetch('/api/auth/password-reset', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  async updatePassword(newPassword: string): Promise<void> {
    if (!API_BASE_URL) {
      throw new Error('Password updates require a configured DuoDesk backend.');
    }
    await apiFetch('/api/auth/update-password', {
      method: 'POST',
      body: JSON.stringify({ new_password: newPassword }),
    });
  },

  async getCurrentWorkspace(): Promise<WorkspaceInfo | null> {
    if (!API_BASE_URL) return null;
    return apiFetch<WorkspaceInfo>('/api/workspaces/current');
  },

  async createWorkspaceInvite(invitedEmail?: string): Promise<InviteInfo> {
    return apiFetch<InviteInfo>('/api/workspaces/invite', {
      method: 'POST',
      body: JSON.stringify({ invitedEmail: invitedEmail?.trim() || undefined }),
    });
  },

  async joinWorkspace(inviteCode: string): Promise<WorkspaceInfo> {
    return apiFetch<WorkspaceInfo>('/api/workspaces/join', {
      method: 'POST',
      body: JSON.stringify({ inviteCode: inviteCode.trim() }),
    });
  },

  async getUsers(): Promise<Record<UserId, UserProfile>> {
    if (API_BASE_URL) {
      if (!inMemoryAccessToken) return {};
      const ws = await apiFetch<WorkspaceInfo>('/api/workspaces/current');
      const map: Record<UserId, UserProfile> = {};
      for (const m of ws.members) {
        map[m.id] = m;
      }
      return map;
    }
    await simulateLatency();
    return loadSnapshot().users;
  },

  async updateUserProfile(userId: UserId, patch: Partial<UserProfile>): Promise<UserProfile> {
    if (API_BASE_URL) {
      return apiFetch<UserProfile>('/api/auth/profile', {
        method: 'PATCH',
        body: JSON.stringify(patch),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.users[userId] = { ...db.users[userId], ...patch };
    saveSnapshot(db);
    return db.users[userId];
  },

  // -------------------------------------------------------------------------
  // TASKS
  // -------------------------------------------------------------------------
  async listTasks(currentUserId: UserId): Promise<TaskItem[]> {
    if (API_BASE_URL) {
      return apiFetch<TaskItem[]>('/api/tasks');
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.tasks.filter((t) => isVisibleToUser(t, currentUserId));
  },

  async createTask(
    currentUserId: UserId,
    input: Omit<TaskItem, 'id' | 'ownerId' | 'createdAt' | 'updatedAt'>
  ): Promise<TaskItem> {
    if (API_BASE_URL) {
      return apiFetch<TaskItem>('/api/tasks', {
        method: 'POST',
        body: JSON.stringify({
          title: input.title,
          description: input.description,
          visibility: input.visibility,
          priority: input.priority,
          dueDate: input.dueDate,
          dueTime: input.dueTime,
          completed: input.completed,
          category: input.category,
        }),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const now = new Date().toISOString();
    const created: TaskItem = {
      ...input,
      id: uid('task'),
      ownerId: currentUserId,
      createdAt: now,
      updatedAt: now,
    };
    db.tasks.unshift(created);

    if (created.visibility === 'shared') {
      db.activities.unshift({
        id: uid('act'),
        actorId: currentUserId,
        action: 'created shared task',
        targetTitle: created.title,
        targetType: 'task',
        visibility: 'shared',
        createdAt: now,
      });
      db.notifications.unshift({
        id: uid('notif'),
        title: `New shared task from ${db.users[currentUserId]?.name ?? 'Teammate'}`,
        message: created.title,
        category: 'shared_task',
        targetRoute: '/shared-space',
        actorId: currentUserId,
        visibility: 'shared',
        read: false,
        createdAt: now,
      });
    }

    saveSnapshot(db);
    return created;
  },

  async updateTask(
    currentUserId: UserId,
    id: string,
    patch: Partial<TaskItem>
  ): Promise<TaskItem> {
    if (API_BASE_URL) {
      return apiFetch<TaskItem>(`/api/tasks/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: JSON.stringify(patch),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const idx = db.tasks.findIndex((t) => t.id === id && isVisibleToUser(t, currentUserId));
    if (idx === -1) throw new Error('Task not found or access denied');
    const now = new Date().toISOString();
    const prev = db.tasks[idx];
    const updated: TaskItem = { ...prev, ...patch, updatedAt: now };
    db.tasks[idx] = updated;

    if (updated.visibility === 'shared' && patch.completed !== undefined && patch.completed !== prev.completed) {
      db.activities.unshift({
        id: uid('act'),
        actorId: currentUserId,
        action: updated.completed ? 'completed' : 'reopened',
        targetTitle: updated.title,
        targetType: 'task',
        visibility: 'shared',
        createdAt: now,
      });
      if (updated.completed) {
        db.notifications.unshift({
          id: uid('notif'),
          title: `${db.users[currentUserId]?.name ?? 'Teammate'} completed a shared task`,
          message: updated.title,
          category: 'completed',
          targetRoute: '/shared-space',
          actorId: currentUserId,
          visibility: 'shared',
          read: false,
          createdAt: now,
        });
      }
    }

    saveSnapshot(db);
    return updated;
  },

  async deleteTask(currentUserId: UserId, id: string): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch(`/api/tasks/${encodeURIComponent(id)}`, { method: 'DELETE' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.tasks = db.tasks.filter((t) => !(t.id === id && isVisibleToUser(t, currentUserId)));
    saveSnapshot(db);
  },

  // -------------------------------------------------------------------------
  // REMINDERS
  // -------------------------------------------------------------------------
  async listReminders(currentUserId: UserId): Promise<ReminderItem[]> {
    if (API_BASE_URL) {
      return apiFetch<ReminderItem[]>('/api/reminders');
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.reminders.filter((r) => isVisibleToUser(r, currentUserId));
  },

  async createReminder(
    currentUserId: UserId,
    input: Omit<ReminderItem, 'id' | 'ownerId' | 'createdAt'>
  ): Promise<ReminderItem> {
    if (API_BASE_URL) {
      return apiFetch<ReminderItem>('/api/reminders', {
        method: 'POST',
        body: JSON.stringify(input),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const now = new Date().toISOString();
    const created: ReminderItem = {
      ...input,
      id: uid('rem'),
      ownerId: currentUserId,
      createdAt: now,
    };
    db.reminders.unshift(created);
    if (created.visibility === 'shared') {
      db.activities.unshift({
        id: uid('act'),
        actorId: currentUserId,
        action: 'scheduled reminder',
        targetTitle: created.title,
        targetType: 'reminder',
        visibility: 'shared',
        createdAt: now,
      });
    }
    saveSnapshot(db);
    return created;
  },

  async updateReminder(
    currentUserId: UserId,
    id: string,
    patch: Partial<ReminderItem>
  ): Promise<ReminderItem> {
    if (API_BASE_URL) {
      return apiFetch<ReminderItem>(`/api/reminders/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: JSON.stringify(patch),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const idx = db.reminders.findIndex((r) => r.id === id && isVisibleToUser(r, currentUserId));
    if (idx === -1) throw new Error('Reminder not found');
    db.reminders[idx] = { ...db.reminders[idx], ...patch };
    saveSnapshot(db);
    return db.reminders[idx];
  },

  async deleteReminder(currentUserId: UserId, id: string): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch(`/api/reminders/${encodeURIComponent(id)}`, { method: 'DELETE' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.reminders = db.reminders.filter((r) => !(r.id === id && isVisibleToUser(r, currentUserId)));
    saveSnapshot(db);
  },

  // -------------------------------------------------------------------------
  // ASSIGNMENTS
  // -------------------------------------------------------------------------
  async listAssignments(currentUserId: UserId): Promise<AssignmentItem[]> {
    if (API_BASE_URL) {
      return apiFetch<AssignmentItem[]>('/api/assignments');
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.assignments.filter((a) => isVisibleToUser(a, currentUserId));
  },

  async createAssignment(
    currentUserId: UserId,
    input: Omit<AssignmentItem, 'id' | 'ownerId' | 'createdAt'>
  ): Promise<AssignmentItem> {
    if (API_BASE_URL) {
      return apiFetch<AssignmentItem>('/api/assignments', {
        method: 'POST',
        body: JSON.stringify({
          title: input.title,
          subject: input.subject,
          description: input.description,
          visibility: input.visibility,
          deadline: input.deadline,
          deadlineTime: input.deadlineTime,
          priority: input.priority,
          status: input.status,
        }),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const now = new Date().toISOString();
    const created: AssignmentItem = {
      ...input,
      id: uid('asg'),
      ownerId: currentUserId,
      createdAt: now,
    };
    db.assignments.unshift(created);
    if (created.visibility === 'shared') {
      db.activities.unshift({
        id: uid('act'),
        actorId: currentUserId,
        action: 'added assignment',
        targetTitle: created.title,
        targetType: 'assignment',
        visibility: 'shared',
        createdAt: now,
      });
      db.notifications.unshift({
        id: uid('notif'),
        title: `New shared assignment from ${db.users[currentUserId]?.name ?? 'Teammate'}`,
        message: `${created.title} (${created.subject})`,
        category: 'shared_task',
        targetRoute: '/shared-space',
        actorId: currentUserId,
        visibility: 'shared',
        read: false,
        createdAt: now,
      });
    }
    saveSnapshot(db);
    return created;
  },

  async updateAssignment(
    currentUserId: UserId,
    id: string,
    patch: Partial<AssignmentItem>
  ): Promise<AssignmentItem> {
    if (API_BASE_URL) {
      return apiFetch<AssignmentItem>(`/api/assignments/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: JSON.stringify(patch),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const idx = db.assignments.findIndex((a) => a.id === id && isVisibleToUser(a, currentUserId));
    if (idx === -1) throw new Error('Assignment not found');
    db.assignments[idx] = { ...db.assignments[idx], ...patch };
    saveSnapshot(db);
    return db.assignments[idx];
  },

  async deleteAssignment(currentUserId: UserId, id: string): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch(`/api/assignments/${encodeURIComponent(id)}`, { method: 'DELETE' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.assignments = db.assignments.filter((a) => !(a.id === id && isVisibleToUser(a, currentUserId)));
    saveSnapshot(db);
  },

  // -------------------------------------------------------------------------
  // DOCUMENTS & ATTACHMENTS
  // -------------------------------------------------------------------------
  async listDocuments(currentUserId: UserId): Promise<DocumentItem[]> {
    if (API_BASE_URL) {
      return apiFetch<DocumentItem[]>('/api/documents');
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.documents.filter((d) => isVisibleToUser(d, currentUserId));
  },

  async uploadDocument(
    currentUserId: UserId,
    input: DocumentUploadInput
  ): Promise<DocumentItem> {
    if (API_BASE_URL) {
      if (!input.file) {
        throw new Error('Please select a file to upload.');
      }
      const form = new FormData();
      form.append('file', input.file, input.name || input.file.name);
      form.append('visibility', input.visibility);
      form.append('entity_type', 'document');
      if (input.summary) {
        form.append('summary', input.summary);
      }
      return apiFetch<DocumentItem>('/api/documents/upload', {
        method: 'POST',
        body: form,
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const now = new Date().toISOString();
    const { file: _ignoredFile, ...docData } = input;
    const created: DocumentItem = {
      ...docData,
      id: uid('doc'),
      ownerId: currentUserId,
      uploadedAt: now,
    };
    db.documents.unshift(created);
    if (created.visibility === 'shared') {
      db.activities.unshift({
        id: uid('act'),
        actorId: currentUserId,
        action: 'uploaded',
        targetTitle: created.name,
        targetType: 'document',
        visibility: 'shared',
        createdAt: now,
      });
      db.notifications.unshift({
        id: uid('notif'),
        title: `${db.users[currentUserId]?.name ?? 'Teammate'} uploaded ${created.name}`,
        message: 'Available now in Shared Space.',
        category: 'file',
        targetRoute: '/shared-space',
        actorId: currentUserId,
        visibility: 'shared',
        read: false,
        createdAt: now,
      });
    }
    saveSnapshot(db);
    return created;
  },

  async uploadAttachment(
    file: File,
    visibility: Visibility,
    entityType: 'task' | 'assignment',
    entityId: string
  ): Promise<DocumentItem> {
    if (!API_BASE_URL) {
      throw new Error('Attachment upload endpoint requires configured backend.');
    }
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('visibility', visibility);
    form.append('entity_type', entityType);
    form.append('entity_id', entityId);
    return apiFetch<DocumentItem>('/api/documents/upload', {
      method: 'POST',
      body: form,
    });
  },

  async getDocumentDownloadUrl(docId: string): Promise<string> {
    const res = await apiFetch<{ url: string }>(`/api/documents/${encodeURIComponent(docId)}/download`);
    return res.url;
  },

  async updateDocument(
    currentUserId: UserId,
    id: string,
    patch: Partial<DocumentItem>
  ): Promise<DocumentItem> {
    if (API_BASE_URL) {
      return apiFetch<DocumentItem>(`/api/documents/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: JSON.stringify({
          visibility: patch.visibility,
          summary: patch.summary,
        }),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const idx = db.documents.findIndex((d) => d.id === id && isVisibleToUser(d, currentUserId));
    if (idx === -1) throw new Error('Document not found');
    db.documents[idx] = { ...db.documents[idx], ...patch };
    saveSnapshot(db);
    return db.documents[idx];
  },

  async deleteDocument(currentUserId: UserId, id: string): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch(`/api/documents/${encodeURIComponent(id)}`, { method: 'DELETE' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.documents = db.documents.filter((d) => !(d.id === id && isVisibleToUser(d, currentUserId)));
    saveSnapshot(db);
  },

  // -------------------------------------------------------------------------
  // NOTES & CHECKLISTS
  // -------------------------------------------------------------------------
  async listNotes(currentUserId: UserId): Promise<NoteItem[]> {
    if (API_BASE_URL) {
      return apiFetch<NoteItem[]>('/api/notes');
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.notes.filter((n) => isVisibleToUser(n, currentUserId));
  },

  async createNote(
    currentUserId: UserId,
    input: Omit<NoteItem, 'id' | 'ownerId' | 'createdAt' | 'updatedAt'>
  ): Promise<NoteItem> {
    if (API_BASE_URL) {
      return apiFetch<NoteItem>('/api/notes', {
        method: 'POST',
        body: JSON.stringify(input),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const now = new Date().toISOString();
    const created: NoteItem = {
      ...input,
      id: uid('note'),
      ownerId: currentUserId,
      createdAt: now,
      updatedAt: now,
    };
    db.notes.unshift(created);
    if (created.visibility === 'shared') {
      db.activities.unshift({
        id: uid('act'),
        actorId: currentUserId,
        action: created.type === 'checklist' ? 'created checklist' : 'shared note',
        targetTitle: created.title,
        targetType: 'note',
        visibility: 'shared',
        createdAt: now,
      });
    }
    saveSnapshot(db);
    return created;
  },

  async updateNote(
    currentUserId: UserId,
    id: string,
    patch: Partial<NoteItem>
  ): Promise<NoteItem> {
    if (API_BASE_URL) {
      return apiFetch<NoteItem>(`/api/notes/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        body: JSON.stringify(patch),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    const idx = db.notes.findIndex((n) => n.id === id && isVisibleToUser(n, currentUserId));
    if (idx === -1) throw new Error('Note not found');
    db.notes[idx] = { ...db.notes[idx], ...patch, updatedAt: new Date().toISOString() };
    saveSnapshot(db);
    return db.notes[idx];
  },

  async deleteNote(currentUserId: UserId, id: string): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch(`/api/notes/${encodeURIComponent(id)}`, { method: 'DELETE' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.notes = db.notes.filter((n) => !(n.id === id && isVisibleToUser(n, currentUserId)));
    saveSnapshot(db);
  },

  // -------------------------------------------------------------------------
  // NOTIFICATIONS & ACTIVITIES
  // -------------------------------------------------------------------------
  async listNotifications(currentUserId: UserId): Promise<NotificationItem[]> {
    if (API_BASE_URL) {
      return apiFetch<NotificationItem[]>('/api/notifications');
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.notifications.filter((n) => isVisibleToUser(n, currentUserId));
  },

  async markNotificationRead(currentUserId: UserId, id: string): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch(`/api/notifications/${encodeURIComponent(id)}/read`, { method: 'POST' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.notifications = db.notifications.map((n) =>
      n.id === id && isVisibleToUser(n, currentUserId) ? { ...n, read: true } : n
    );
    saveSnapshot(db);
  },

  async markAllNotificationsRead(currentUserId: UserId): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch('/api/notifications/read-all', { method: 'POST' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.notifications = db.notifications.map((n) =>
      isVisibleToUser(n, currentUserId) ? { ...n, read: true } : n
    );
    saveSnapshot(db);
  },

  async clearNotifications(currentUserId: UserId): Promise<void> {
    if (API_BASE_URL) {
      await apiFetch('/api/notifications', { method: 'DELETE' });
      return;
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.notifications = db.notifications.filter((n) => !isVisibleToUser(n, currentUserId));
    saveSnapshot(db);
  },

  async listActivities(currentUserId: UserId): Promise<ActivityEvent[]> {
    if (API_BASE_URL) {
      return [];
    }
    await simulateLatency();
    const db = loadSnapshot();
    return db.activities.filter((a) => a.visibility === 'shared' || a.actorId === currentUserId);
  },

  async getNotificationSettings(userId: UserId): Promise<NotificationSettings> {
    if (API_BASE_URL) {
      return apiFetch<NotificationSettings>('/api/notifications/preferences');
    }
    const db = loadSnapshot();
    return db.notificationSettings[userId];
  },

  async updateNotificationSettings(
    userId: UserId,
    settings: NotificationSettings
  ): Promise<NotificationSettings> {
    if (API_BASE_URL) {
      return apiFetch<NotificationSettings>('/api/notifications/preferences', {
        method: 'PUT',
        body: JSON.stringify(settings),
      });
    }
    await simulateLatency();
    const db = loadSnapshot();
    db.notificationSettings[userId] = settings;
    saveSnapshot(db);
    return settings;
  },

  async resetDemoData(): Promise<void> {
    if (API_BASE_URL) return;
    await simulateLatency();
    resetSnapshot();
  },
};
