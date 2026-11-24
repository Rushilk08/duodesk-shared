import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { DocumentUploadInput, duodeskApi } from '../services/apiClient';
import { subscribeToWorkspaceRealtime } from '../services/realtimeClient';
import {
  AssignmentItem,
  DocumentItem,
  NoteItem,
  NotificationSettings,
  ReminderItem,
  TaskItem,
  UserId,
  UserProfile,
  Visibility,
} from '../types/duodesk';

export function useDuoDeskQueries() {
  const { currentUserId, accessToken, workspaceId, isBackendConfigured } = useWorkspace();
  const qc = useQueryClient();
  const activeId: UserId = currentUserId ?? '';
  const enabled = Boolean(currentUserId) && (!isBackendConfigured || Boolean(accessToken));

  useEffect(() => {
    if (!isBackendConfigured || !accessToken || !workspaceId) return;
    return subscribeToWorkspaceRealtime(accessToken, workspaceId, () => {
      qc.invalidateQueries({ queryKey: ['duodesk'] });
    });
  }, [accessToken, isBackendConfigured, qc, workspaceId]);

  const usersQuery = useQuery({
    queryKey: ['duodesk', 'users', activeId],
    queryFn: () => duodeskApi.getUsers(),
    enabled: !isBackendConfigured || enabled,
  });

  const workspaceQuery = useQuery({
    queryKey: ['duodesk', 'workspace', activeId],
    queryFn: () => duodeskApi.getCurrentWorkspace(),
    enabled: isBackendConfigured && enabled,
  });

  const tasksQuery = useQuery({
    queryKey: ['duodesk', 'tasks', activeId],
    queryFn: () => duodeskApi.listTasks(activeId),
    enabled,
  });

  const remindersQuery = useQuery({
    queryKey: ['duodesk', 'reminders', activeId],
    queryFn: () => duodeskApi.listReminders(activeId),
    enabled,
  });

  const assignmentsQuery = useQuery({
    queryKey: ['duodesk', 'assignments', activeId],
    queryFn: () => duodeskApi.listAssignments(activeId),
    enabled,
  });

  const documentsQuery = useQuery({
    queryKey: ['duodesk', 'documents', activeId],
    queryFn: () => duodeskApi.listDocuments(activeId),
    enabled,
  });

  const notesQuery = useQuery({
    queryKey: ['duodesk', 'notes', activeId],
    queryFn: () => duodeskApi.listNotes(activeId),
    enabled,
  });

  const notificationsQuery = useQuery({
    queryKey: ['duodesk', 'notifications', activeId],
    queryFn: () => duodeskApi.listNotifications(activeId),
    enabled,
  });

  const activitiesQuery = useQuery({
    queryKey: ['duodesk', 'activities', activeId],
    queryFn: () => duodeskApi.listActivities(activeId),
    enabled,
  });

  const notifSettingsQuery = useQuery({
    queryKey: ['duodesk', 'notifSettings', activeId],
    queryFn: () => duodeskApi.getNotificationSettings(activeId),
    enabled,
  });

  return {
    usersQuery,
    workspaceQuery,
    tasksQuery,
    remindersQuery,
    assignmentsQuery,
    documentsQuery,
    notesQuery,
    notificationsQuery,
    activitiesQuery,
    notifSettingsQuery,
  };
}

export function useDuoDeskMutations() {
  const qc = useQueryClient();
  const { currentUserId, pushToast } = useWorkspace();
  const activeId: UserId = currentUserId ?? '';

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['duodesk'] });
  };

  const handleError = (err: unknown) => {
    const message = err instanceof Error ? err.message : 'Something went wrong.';
    pushToast({
      title: 'Action failed',
      description: message,
      tone: 'warning',
    });
  };

  const createTask = useMutation({
    mutationFn: (input: Omit<TaskItem, 'id' | 'ownerId' | 'createdAt' | 'updatedAt'>) =>
      duodeskApi.createTask(activeId, input),
    onSuccess: (created) => {
      invalidateAll();
      pushToast({
        title: 'Task created',
        description: `${created.title} (${created.visibility === 'shared' ? 'Shared Space' : 'My Space'})`,
        tone: 'success',
      });
    },
    onError: handleError,
  });

  const updateTask = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<TaskItem> }) =>
      duodeskApi.updateTask(activeId, id, patch),
    onSuccess: () => invalidateAll(),
    onError: handleError,
  });

  const deleteTask = useMutation({
    mutationFn: (id: string) => duodeskApi.deleteTask(activeId, id),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Task removed', tone: 'neutral' });
    },
    onError: handleError,
  });

  const createReminder = useMutation({
    mutationFn: (input: Omit<ReminderItem, 'id' | 'ownerId' | 'createdAt'>) =>
      duodeskApi.createReminder(activeId, input),
    onSuccess: (created) => {
      invalidateAll();
      pushToast({
        title: 'Reminder scheduled',
        description: created.title,
        tone: 'success',
      });
    },
    onError: handleError,
  });

  const updateReminder = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<ReminderItem> }) =>
      duodeskApi.updateReminder(activeId, id, patch),
    onSuccess: () => invalidateAll(),
    onError: handleError,
  });

  const deleteReminder = useMutation({
    mutationFn: (id: string) => duodeskApi.deleteReminder(activeId, id),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Reminder deleted', tone: 'neutral' });
    },
    onError: handleError,
  });

  const createAssignment = useMutation({
    mutationFn: (input: Omit<AssignmentItem, 'id' | 'ownerId' | 'createdAt'>) =>
      duodeskApi.createAssignment(activeId, input),
    onSuccess: (created) => {
      invalidateAll();
      pushToast({
        title: 'Assignment added',
        description: created.title,
        tone: 'success',
      });
    },
    onError: handleError,
  });

  const updateAssignment = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<AssignmentItem> }) =>
      duodeskApi.updateAssignment(activeId, id, patch),
    onSuccess: () => invalidateAll(),
    onError: handleError,
  });

  const deleteAssignment = useMutation({
    mutationFn: (id: string) => duodeskApi.deleteAssignment(activeId, id),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Assignment deleted', tone: 'neutral' });
    },
    onError: handleError,
  });

  const uploadDocument = useMutation({
    mutationFn: (input: DocumentUploadInput) => duodeskApi.uploadDocument(activeId, input),
    onSuccess: (created) => {
      invalidateAll();
      pushToast({
        title: 'File uploaded',
        description: `${created.name} saved to ${created.visibility === 'shared' ? 'Shared Space' : 'My Space'}`,
        tone: 'success',
      });
    },
    onError: handleError,
  });

  const uploadAttachment = useMutation({
    mutationFn: ({
      file,
      visibility,
      entityType,
      entityId,
    }: {
      file: File;
      visibility: Visibility;
      entityType: 'task' | 'assignment';
      entityId: string;
    }) => duodeskApi.uploadAttachment(file, visibility, entityType, entityId),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Attachment uploaded', tone: 'success' });
    },
    onError: handleError,
  });

  const updateDocument = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<DocumentItem> }) =>
      duodeskApi.updateDocument(activeId, id, patch),
    onSuccess: () => invalidateAll(),
    onError: handleError,
  });

  const deleteDocument = useMutation({
    mutationFn: (id: string) => duodeskApi.deleteDocument(activeId, id),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Document deleted', tone: 'neutral' });
    },
    onError: handleError,
  });

  const createNote = useMutation({
    mutationFn: (input: Omit<NoteItem, 'id' | 'ownerId' | 'createdAt' | 'updatedAt'>) =>
      duodeskApi.createNote(activeId, input),
    onSuccess: (created) => {
      invalidateAll();
      pushToast({
        title: created.type === 'checklist' ? 'Checklist created' : 'Note saved',
        description: created.title,
        tone: 'success',
      });
    },
    onError: handleError,
  });

  const updateNote = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<NoteItem> }) =>
      duodeskApi.updateNote(activeId, id, patch),
    onSuccess: () => invalidateAll(),
    onError: handleError,
  });

  const deleteNote = useMutation({
    mutationFn: (id: string) => duodeskApi.deleteNote(activeId, id),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Note deleted', tone: 'neutral' });
    },
    onError: handleError,
  });

  const markNotificationRead = useMutation({
    mutationFn: (id: string) => duodeskApi.markNotificationRead(activeId, id),
    onSuccess: () => invalidateAll(),
    onError: handleError,
  });

  const markAllNotificationsRead = useMutation({
    mutationFn: () => duodeskApi.markAllNotificationsRead(activeId),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'All notifications marked as read', tone: 'success' });
    },
    onError: handleError,
  });

  const clearNotifications = useMutation({
    mutationFn: () => duodeskApi.clearNotifications(activeId),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Notifications cleared', tone: 'neutral' });
    },
    onError: handleError,
  });

  const updateUserProfile = useMutation({
    mutationFn: ({ userId, patch }: { userId: UserId; patch: Partial<UserProfile> }) =>
      duodeskApi.updateUserProfile(userId, patch),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Profile updated', tone: 'success' });
    },
    onError: handleError,
  });

  const updateNotificationSettings = useMutation({
    mutationFn: (settings: NotificationSettings) =>
      duodeskApi.updateNotificationSettings(activeId, settings),
    onSuccess: () => {
      invalidateAll();
      pushToast({ title: 'Notification preferences saved', tone: 'success' });
    },
    onError: handleError,
  });

  const resetDemoData = useMutation({
    mutationFn: () => duodeskApi.resetDemoData(),
    onSuccess: () => {
      invalidateAll();
      pushToast({
        title: 'Demonstration data restored',
        description: 'All sample tasks, assignments, and documents reset.',
        tone: 'success',
      });
    },
    onError: handleError,
  });

  return {
    createTask,
    updateTask,
    deleteTask,
    createReminder,
    updateReminder,
    deleteReminder,
    createAssignment,
    updateAssignment,
    deleteAssignment,
    uploadDocument,
    uploadAttachment,
    updateDocument,
    deleteDocument,
    createNote,
    updateNote,
    deleteNote,
    markNotificationRead,
    markAllNotificationsRead,
    clearNotifications,
    updateUserProfile,
    updateNotificationSettings,
    resetDemoData,
  };
}
