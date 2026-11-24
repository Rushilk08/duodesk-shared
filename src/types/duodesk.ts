export type UserId = string;

export type Visibility = 'private' | 'shared';

export type Priority = 'low' | 'medium' | 'high';

export type AssignmentStatus = 'pending' | 'in_progress' | 'completed';

export type RepeatOption = 'none' | 'daily' | 'weekly' | 'monthly';

export type NotificationPreference = 'push_in_app' | 'in_app' | 'email_digest';

export type DocumentKind = 'pdf' | 'doc' | 'image';

export type ThemeMode = 'light' | 'dark';

export type WorkspaceScope = 'all' | 'personal' | 'shared';

export interface UserProfile {
  id: UserId;
  name: string;
  fullName: string;
  email: string;
  role: string;
  avatarUrl: string;
  accentHex: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken?: string | null;
  user: UserProfile;
  workspaceId?: string | null;
}

export interface WorkspaceInfo {
  id: string;
  name: string;
  ownerId: string;
  members: UserProfile[];
}

export interface InviteInfo {
  inviteCode: string;
  inviteUrl: string;
  expiresAt: string;
  workspaceId: string;
}

export interface AttachmentRef {
  id: string;
  name: string;
  sizeBytes: number;
  kind: DocumentKind;
  mimeType: string;
  previewUrl?: string;
  downloadUrl?: string;
}

export interface TaskItem {
  id: string;
  title: string;
  description?: string;
  ownerId: UserId;
  lastModifiedBy?: UserId | null;
  visibility: Visibility;
  priority: Priority;
  dueDate: string; // YYYY-MM-DD
  dueTime?: string; // HH:mm
  completed: boolean;
  category?: string;
  attachments: AttachmentRef[];
  createdAt: string;
  updatedAt: string;
}

export interface ReminderItem {
  id: string;
  title: string;
  description: string;
  ownerId: UserId;
  lastModifiedBy?: UserId | null;
  visibility: Visibility;
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  priority: Priority;
  repeat: RepeatOption;
  notificationPref: NotificationPreference;
  completed: boolean;
  createdAt: string;
}

export interface AssignmentItem {
  id: string;
  title: string;
  subject: string;
  description: string;
  ownerId: UserId;
  lastModifiedBy?: UserId | null;
  visibility: Visibility;
  deadline: string; // YYYY-MM-DD
  deadlineTime?: string; // HH:mm
  priority: Priority;
  status: AssignmentStatus;
  attachments: AttachmentRef[];
  createdAt: string;
}

export interface DocumentItem {
  id: string;
  name: string;
  kind: DocumentKind;
  extension: string;
  mimeType: string;
  sizeBytes: number;
  ownerId: UserId;
  lastModifiedBy?: UserId | null;
  visibility: Visibility;
  uploadedAt: string; // ISO date string
  previewUrl?: string;
  downloadUrl?: string;
  summary?: string;
}

export interface ChecklistRow {
  id: string;
  text: string;
  checked: boolean;
}

export interface NoteItem {
  id: string;
  title: string;
  content: string;
  type: 'note' | 'checklist';
  checklist: ChecklistRow[];
  ownerId: UserId;
  lastModifiedBy?: UserId | null;
  visibility: Visibility;
  updatedAt: string;
  createdAt: string;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  category: 'deadline' | 'reminder' | 'shared_task' | 'file' | 'completed';
  targetRoute: string;
  actorId: UserId;
  visibility: Visibility;
  ownerId?: UserId; // when private
  read: boolean;
  createdAt: string;
}

export interface ActivityEvent {
  id: string;
  actorId: UserId;
  action: string;
  targetTitle: string;
  targetType: 'task' | 'assignment' | 'reminder' | 'document' | 'note';
  visibility: Visibility;
  createdAt: string;
}

export interface NotificationSettings {
  deadlineAlerts: boolean;
  sharedUpdates: boolean;
  fileUploads: boolean;
  dailyDigest: boolean;
}

export interface WorkspaceStateSnapshot {
  users: Record<UserId, UserProfile>;
  tasks: TaskItem[];
  reminders: ReminderItem[];
  assignments: AssignmentItem[];
  documents: DocumentItem[];
  notes: NoteItem[];
  notifications: NotificationItem[];
  activities: ActivityEvent[];
  notificationSettings: Record<UserId, NotificationSettings>;
}
