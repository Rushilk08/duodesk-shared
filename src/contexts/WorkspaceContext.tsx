import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { duodeskApi, setApiAccessToken } from '../services/apiClient';
import {
  AuthSession,
  InviteInfo,
  ThemeMode,
  UserId,
  Visibility,
  WorkspaceInfo,
} from '../types/duodesk';

export type CreateModalKind = 'task' | 'reminder' | 'assignment' | 'note' | 'document';

export interface CreateDefaults {
  kind?: CreateModalKind;
  visibility?: Visibility;
  date?: string;
}

export interface ToastMessage {
  id: string;
  title: string;
  description?: string;
  tone?: 'neutral' | 'success' | 'warning';
}

interface WorkspaceContextValue {
  currentUserId: UserId | null;
  accessToken: string | null;
  workspaceId: string | null;
  isBackendConfigured: boolean;
  pendingInviteCode: string;
  setCurrentUserId: (id: UserId | null) => void;
  switchUser: (id: UserId) => void;
  login: (email: string, password: string) => Promise<AuthSession>;
  register: (payload: {
    email: string;
    password: string;
    name: string;
    fullName?: string;
    role?: string;
    inviteCode?: string;
  }) => Promise<AuthSession>;
  requestPasswordReset: (email: string) => Promise<void>;
  updatePassword: (newPassword: string) => Promise<void>;
  createInvite: (invitedEmail?: string) => Promise<InviteInfo>;
  joinWorkspace: (inviteCode: string) => Promise<WorkspaceInfo>;
  logout: () => void;
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  toggleTheme: () => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  createDefaults: CreateDefaults;
  setCreateDefaults: (defaults: CreateDefaults) => void;
  toasts: ToastMessage[];
  pushToast: (toast: Omit<ToastMessage, 'id'>) => void;
  dismissToast: (id: string) => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | undefined>(undefined);

const USER_STORAGE_KEY = 'duodesk_active_user_v1';
const THEME_STORAGE_KEY = 'duodesk_theme_v1';

export function WorkspaceProvider({ children }: { children: React.ReactNode }) {
  const isBackendConfigured = duodeskApi.isBackendConfigured;

  const [pendingInviteCode] = useState<string>(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('invite')?.trim() || '';
  });

  // Never store JWTs or backend sessions in localStorage; only demo mode remembers 'rushil' | 'alex'
  const [currentUserId, setCurrentUserIdState] = useState<UserId | null>(() => {
    if (isBackendConfigured) return null;
    const saved = localStorage.getItem(USER_STORAGE_KEY);
    if (saved === 'rushil' || saved === 'alex') return saved;
    if (saved === 'logged_out') return null;
    return 'rushil';
  });

  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);

  const [theme, setThemeState] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    return saved === 'dark' ? 'dark' : 'light';
  });

  const [searchQuery, setSearchQuery] = useState('');
  const [createDefaults, setCreateDefaults] = useState<CreateDefaults>({
    kind: 'task',
    visibility: 'private',
  });
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const setCurrentUserId = useCallback(
    (id: UserId | null) => {
      setCurrentUserIdState(id);
      if (!isBackendConfigured) {
        if (id) {
          localStorage.setItem(USER_STORAGE_KEY, id);
        } else {
          localStorage.setItem(USER_STORAGE_KEY, 'logged_out');
        }
      }
    },
    [isBackendConfigured]
  );

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const pushToast = useCallback(
    (toast: Omit<ToastMessage, 'id'>) => {
      const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
      setToasts((prev) => [...prev.slice(-2), { ...toast, id }]);
      window.setTimeout(() => {
        dismissToast(id);
      }, 3400);
    },
    [dismissToast]
  );

  const switchUser = useCallback(
    (id: UserId) => {
      if (isBackendConfigured) return;
      setCurrentUserId(id);
      setSearchQuery('');
      pushToast({
        title: `Switched to ${id === 'rushil' ? 'Rushil' : 'Alex'}`,
        description: 'Private space updated. Shared items stay synced.',
        tone: 'success',
      });
    },
    [isBackendConfigured, pushToast, setCurrentUserId]
  );

  const applyAuthSession = useCallback(
    (session: AuthSession) => {
      if (session.accessToken) {
        setApiAccessToken(session.accessToken);
        setAccessToken(session.accessToken);
        setWorkspaceId(session.workspaceId ?? null);
        setCurrentUserId(session.user.id);
      }
    },
    [setCurrentUserId]
  );

  const login = useCallback(
    async (email: string, password: string): Promise<AuthSession> => {
      const session = await duodeskApi.login(email, password);
      applyAuthSession(session);
      pushToast({
        title: `Welcome back, ${session.user.name}`,
        description: 'Your personal space and Shared Space are synced.',
        tone: 'success',
      });
      return session;
    },
    [applyAuthSession, pushToast]
  );

  const register = useCallback(
    async (payload: {
      email: string;
      password: string;
      name: string;
      fullName?: string;
      role?: string;
      inviteCode?: string;
    }): Promise<AuthSession> => {
      const session = await duodeskApi.register(payload);
      if (session.accessToken) {
        applyAuthSession(session);
        pushToast({
          title: `Welcome to DuoDesk, ${session.user.name}`,
          description: payload.inviteCode
            ? 'You joined the shared DuoDesk workspace.'
            : 'Your private space and new DuoDesk workspace are ready.',
          tone: 'success',
        });
      } else {
        pushToast({
          title: 'Account created',
          description: 'Please confirm your email address, then sign in.',
          tone: 'success',
        });
      }
      return session;
    },
    [applyAuthSession, pushToast]
  );

  const requestPasswordReset = useCallback(
    async (email: string) => {
      await duodeskApi.requestPasswordReset(email);
      pushToast({
        title: 'Password reset sent',
        description: `Check ${email} for your reset link.`,
        tone: 'success',
      });
    },
    [pushToast]
  );

  const updatePassword = useCallback(
    async (newPassword: string) => {
      await duodeskApi.updatePassword(newPassword);
      pushToast({
        title: 'Password updated',
        tone: 'success',
      });
    },
    [pushToast]
  );

  const createInvite = useCallback(
    async (invitedEmail?: string): Promise<InviteInfo> => {
      const invite = await duodeskApi.createWorkspaceInvite(invitedEmail);
      pushToast({
        title: 'Invitation code ready',
        description: `Share ${invite.inviteCode} with your partner.`,
        tone: 'success',
      });
      return invite;
    },
    [pushToast]
  );

  const joinWorkspace = useCallback(
    async (inviteCode: string): Promise<WorkspaceInfo> => {
      const ws = await duodeskApi.joinWorkspace(inviteCode);
      setWorkspaceId(ws.id);
      pushToast({
        title: 'Joined DuoDesk Shared Space',
        description: `Connected with ${ws.members.length}/2 members.`,
        tone: 'success',
      });
      return ws;
    },
    [pushToast]
  );

  const logout = useCallback(() => {
    void duodeskApi.logout();
    setApiAccessToken(null);
    setAccessToken(null);
    setWorkspaceId(null);
    setCurrentUserId(null);
  }, [setCurrentUserId]);

  const setTheme = useCallback((next: ThemeMode) => {
    setThemeState(next);
  }, []);

  const toggleTheme = useCallback(() => {
    setThemeState((prev) => (prev === 'light' ? 'dark' : 'light'));
  }, []);

  return (
    <WorkspaceContext.Provider
      value={{
        currentUserId,
        accessToken,
        workspaceId,
        isBackendConfigured,
        pendingInviteCode,
        setCurrentUserId,
        switchUser,
        login,
        register,
        requestPasswordReset,
        updatePassword,
        createInvite,
        joinWorkspace,
        logout,
        theme,
        setTheme,
        toggleTheme,
        searchQuery,
        setSearchQuery,
        createDefaults,
        setCreateDefaults,
        toasts,
        pushToast,
        dismissToast,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  const ctx = useContext(WorkspaceContext);
  if (!ctx) throw new Error('useWorkspace must be used inside WorkspaceProvider');
  return ctx;
}
