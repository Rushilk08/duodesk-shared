import React, { useEffect, useState } from 'react';
import {
  ArrowLeftRight,
  Bell,
  Check,
  CheckCheck,
  Copy,
  KeyRound,
  LogOut,
  Moon,
  RotateCcw,
  Sun,
  Trash2,
  UserPlus,
  Users,
} from 'lucide-react';
import { UserAvatar } from '../components/UserAvatar';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskMutations, useDuoDeskQueries } from '../hooks/useDuoDeskData';
import { InviteInfo, NotificationSettings, UserId } from '../types/duodesk';
import { formatRelativeTime } from '../utils/formatters';

export function ProfilePage() {
  const {
    currentUserId,
    isBackendConfigured,
    switchUser,
    createInvite,
    joinWorkspace,
    updatePassword,
    logout,
    theme,
    setTheme,
    pushToast,
  } = useWorkspace();
  const { usersQuery, workspaceQuery, notificationsQuery, notifSettingsQuery } = useDuoDeskQueries();
  const mutations = useDuoDeskMutations();

  const users = usersQuery.data;
  const currentUser = currentUserId && users ? users[currentUserId] : undefined;
  const otherUserId: UserId = currentUserId === 'rushil' ? 'alex' : 'rushil';
  const otherUser = !isBackendConfigured && users ? users[otherUserId] : undefined;

  const workspace = workspaceQuery.data;
  const members = workspace?.members ?? (users ? Object.values(users) : []);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [partnerEmail, setPartnerEmail] = useState('');
  const [inviteResult, setInviteResult] = useState<InviteInfo | null>(null);
  const [joinCode, setJoinCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [notifPrefs, setNotifPrefs] = useState<NotificationSettings>({
    deadlineAlerts: true,
    sharedUpdates: true,
    fileUploads: true,
    dailyDigest: false,
  });

  useEffect(() => {
    if (currentUser) {
      setName(currentUser.name);
      setEmail(currentUser.email);
      setRole(currentUser.role);
    }
  }, [currentUser]);

  useEffect(() => {
    if (notifSettingsQuery.data) {
      setNotifPrefs(notifSettingsQuery.data);
    }
  }, [notifSettingsQuery.data]);

  const notifications = notificationsQuery.data ?? [];
  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUserId) return;
    await mutations.updateUserProfile.mutateAsync({
      userId: currentUserId,
      patch: {
        name: name.trim(),
        email: email.trim(),
        role: role.trim(),
      },
    });
  };

  const handleGenerateInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const inv = await createInvite(partnerEmail.trim() || undefined);
      setInviteResult(inv);
    } catch (err) {
      pushToast({
        title: 'Unable to create invite',
        description: err instanceof Error ? err.message : 'Workspace already has 2 members.',
        tone: 'warning',
      });
    }
  };

  const handleJoinByCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinCode.trim()) return;
    try {
      await joinWorkspace(joinCode.trim());
      setJoinCode('');
      await workspaceQuery.refetch();
      await usersQuery.refetch();
    } catch (err) {
      pushToast({
        title: 'Unable to join workspace',
        description: err instanceof Error ? err.message : 'Invalid invite code.',
        tone: 'warning',
      });
    }
  };

  const handlePasswordUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 8) return;
    try {
      await updatePassword(newPassword);
      setNewPassword('');
    } catch (err) {
      pushToast({
        title: 'Password update failed',
        description: err instanceof Error ? err.message : 'Try again.',
        tone: 'warning',
      });
    }
  };

  const toggleNotifPref = async (key: keyof NotificationSettings) => {
    const next = { ...notifPrefs, [key]: !notifPrefs[key] };
    setNotifPrefs(next);
    await mutations.updateNotificationSettings.mutateAsync(next);
    pushToast({ title: 'Preferences updated', tone: 'success' });
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      {/* User Card */}
      <section className="flex flex-col items-start justify-between gap-5 rounded-3xl border border-[#EAE4DC] bg-gradient-to-br from-white via-[#FFF9FA] to-[#FCE6D4]/55 p-6 sm:flex-row sm:items-center dark:border-[#2E2D3B] dark:from-[#1F1F29] dark:to-[#2C2424]">
        <div className="flex items-center gap-4">
          <UserAvatar user={currentUser} size="lg" />
          <div>
            <h1 className="font-display text-xl font-bold text-[#18181F] dark:text-white">
              {currentUser?.fullName}
            </h1>
            <p className="text-xs text-[#6E6A75] dark:text-[#A19CA8]">
              {currentUser?.email} ·{' '}
              {isBackendConfigured
                ? currentUser?.role || 'Member'
                : currentUserId === 'rushil'
                ? 'User A'
                : 'User B'}
            </p>
          </div>
        </div>

        {otherUser && (
          <button
            type="button"
            onClick={() => switchUser(otherUser.id)}
            className="inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-xs font-semibold text-[#18181F] shadow-2xs transition-colors hover:bg-[#E6DFFB] dark:bg-[#272733] dark:text-white"
          >
            <ArrowLeftRight className="h-3.5 w-3.5 text-[#9B82F3]" />
            <span>Switch to {otherUser.name}</span>
          </button>
        )}
      </section>

      {/* Profile Form */}
      <section className="rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
        <h2 className="mb-4 font-display text-base font-bold text-[#18181F] dark:text-white">
          Profile details
        </h2>
        <form onSubmit={handleSaveProfile} className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-xs font-medium text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-xs font-medium text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Role</label>
              <input
                type="text"
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-xs font-medium text-[#18181F] focus:border-[#9B82F3] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>
          </div>
          <div className="flex justify-end">
            <button
              type="submit"
              className="rounded-full bg-[#18181F] px-5 py-2 text-xs font-semibold text-white hover:opacity-90 dark:bg-[#9B82F3] dark:text-[#16161D]"
            >
              Save profile
            </button>
          </div>
        </form>
      </section>

      {/* Two-User Shared Workspace Management (Backend mode) */}
      {isBackendConfigured && (
        <section className="space-y-4 rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users className="h-4 w-4 text-[#9B82F3]" />
              <h2 className="font-display text-base font-bold text-[#18181F] dark:text-white">
                DuoDesk Members ({members.length} / 2)
              </h2>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {members.map((m) => (
              <div
                key={m.id}
                className="flex items-center gap-3 rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] p-3 dark:border-[#343343] dark:bg-[#272733]"
              >
                <UserAvatar user={m} size="sm" />
                <div className="min-w-0">
                  <p className="truncate text-xs font-semibold text-[#18181F] dark:text-white">
                    {m.name}
                  </p>
                  <p className="truncate text-[11px] text-[#6E6A75]">{m.email}</p>
                </div>
              </div>
            ))}
          </div>

          {members.length < 2 && (
            <div className="space-y-3 border-t border-[#F2EDE6] pt-4 dark:border-[#2E2D3B]">
              <form onSubmit={handleGenerateInvite} className="flex flex-wrap gap-2">
                <input
                  type="email"
                  value={partnerEmail}
                  onChange={(e) => setPartnerEmail(e.target.value)}
                  placeholder="Partner email (optional)"
                  className="flex-1 rounded-full border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2 text-xs text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                />
                <button
                  type="submit"
                  className="inline-flex items-center gap-1.5 rounded-full bg-[#9B82F3] px-4 py-2 text-xs font-semibold text-white hover:opacity-90"
                >
                  <UserPlus className="h-3.5 w-3.5" />
                  <span>Invite 2nd member</span>
                </button>
              </form>

              {inviteResult && (
                <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl bg-[#E6DFFB]/45 px-4 py-3 text-xs dark:bg-[#2D283E]">
                  <div>
                    <p className="font-semibold text-[#5B469C] dark:text-[#E6DFFB]">
                      Invite Code: <span className="font-mono">{inviteResult.inviteCode}</span>
                    </p>
                    <p className="mt-0.5 truncate text-[11px] text-[#6E6A75]">
                      {inviteResult.inviteUrl}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      void navigator.clipboard.writeText(inviteResult.inviteUrl);
                      pushToast({ title: 'Invite link copied', tone: 'success' });
                    }}
                    className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-[#18181F] dark:bg-[#1F1F29] dark:text-white"
                  >
                    <Copy className="h-3 w-3" />
                    <span>Copy link</span>
                  </button>
                </div>
              )}

              <form onSubmit={handleJoinByCode} className="flex flex-wrap gap-2">
                <input
                  type="text"
                  value={joinCode}
                  onChange={(e) => setJoinCode(e.target.value)}
                  placeholder="Or paste partner invite code (DUO-...)"
                  className="flex-1 rounded-full border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2 font-mono text-xs text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                />
                <button
                  type="submit"
                  className="rounded-full border border-[#EAE4DC] bg-white px-4 py-2 text-xs font-semibold text-[#18181F] hover:bg-[#FAF7F2] dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                >
                  Join workspace
                </button>
              </form>
            </div>
          )}

          <form
            onSubmit={handlePasswordUpdate}
            className="flex flex-wrap items-center gap-2 border-t border-[#F2EDE6] pt-4 dark:border-[#2E2D3B]"
          >
            <KeyRound className="h-3.5 w-3.5 text-[#6E6A75]" />
            <input
              type="password"
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="New password (8+ characters)"
              className="flex-1 rounded-full border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2 text-xs text-[#18181F] focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
            />
            <button
              type="submit"
              className="rounded-full bg-[#18181F] px-4 py-2 text-xs font-semibold text-white hover:opacity-90 dark:bg-[#9B82F3] dark:text-[#16161D]"
            >
              Update password
            </button>
          </form>
        </section>
      )}

      {/* Notifications Center */}
      <section className="rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Bell className="h-4 w-4 text-[#E684A7]" />
            <h2 className="font-display text-base font-bold text-[#18181F] dark:text-white">
              Notifications ({unreadCount} unread)
            </h2>
          </div>
          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={() => mutations.markAllNotificationsRead.mutate()}
                className="inline-flex items-center gap-1 rounded-full bg-[#FAF7F2] px-3 py-1.5 text-xs font-semibold text-[#5B469C] hover:bg-[#E6DFFB] dark:bg-[#272733] dark:text-[#D5C9FA]"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                <span>Mark all read</span>
              </button>
            )}
            {notifications.length > 0 && (
              <button
                type="button"
                onClick={() => mutations.clearNotifications.mutate()}
                className="rounded-full p-1.5 text-[#6E6A75] hover:text-[#DC2626]"
                aria-label="Clear all notifications"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        </div>

        {notifications.length === 0 ? (
          <p className="py-6 text-center text-xs text-[#6E6A75]">No notifications right now.</p>
        ) : (
          <div className="divide-y divide-[#F2EDE6] dark:divide-[#2E2D3B]">
            {notifications.map((n) => (
              <div key={n.id} className="flex items-start justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p
                    className={`text-xs ${
                      n.read
                        ? 'font-medium text-[#6E6A75]'
                        : 'font-semibold text-[#18181F] dark:text-white'
                    }`}
                  >
                    {n.title}
                  </p>
                  <p className="mt-0.5 text-[11px] text-[#6E6A75]">{n.message}</p>
                  <p className="mt-1 font-mono text-[10px] text-[#9A94A3]">
                    {formatRelativeTime(n.createdAt)}
                  </p>
                </div>
                {!n.read && (
                  <button
                    type="button"
                    onClick={() => mutations.markNotificationRead.mutate(n.id)}
                    className="rounded-full p-1.5 text-[#9B82F3] hover:bg-[#E6DFFB]/60"
                    aria-label="Mark as read"
                  >
                    <Check className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Theme & Notification Toggles */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
        <section className="rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
          <h2 className="mb-3 font-display text-base font-bold text-[#18181F] dark:text-white">
            Appearance
          </h2>
          <div className="grid grid-cols-2 gap-2.5">
            <button
              type="button"
              onClick={() => setTheme('light')}
              className={`flex items-center justify-center gap-2 rounded-full border py-2.5 text-xs font-semibold ${
                theme === 'light'
                  ? 'border-[#E684A7] bg-[#F8D8E4]/65 text-[#8E3B5B]'
                  : 'border-[#EAE4DC] text-[#6E6A75] dark:border-[#343343]'
              }`}
            >
              <Sun className="h-3.5 w-3.5" />
              <span>Warm Cream</span>
            </button>
            <button
              type="button"
              onClick={() => setTheme('dark')}
              className={`flex items-center justify-center gap-2 rounded-full border py-2.5 text-xs font-semibold ${
                theme === 'dark'
                  ? 'border-[#9B82F3] bg-[#2D283E] text-white'
                  : 'border-[#EAE4DC] text-[#6E6A75] dark:border-[#343343]'
              }`}
            >
              <Moon className="h-3.5 w-3.5" />
              <span>Soft Dark</span>
            </button>
          </div>
        </section>

        <section className="rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
          <h2 className="mb-3 font-display text-base font-bold text-[#18181F] dark:text-white">
            Alert Preferences
          </h2>
          <div className="space-y-2.5">
            {(
              [
                { key: 'deadlineAlerts', label: 'Deadline reminders' },
                { key: 'sharedUpdates', label: 'Shared space updates' },
                { key: 'fileUploads', label: 'File upload alerts' },
              ] as const
            ).map((item) => (
              <label
                key={item.key}
                className="flex cursor-pointer items-center justify-between text-xs font-medium text-[#18181F] dark:text-white"
              >
                <span>{item.label}</span>
                <input
                  type="checkbox"
                  checked={notifPrefs[item.key]}
                  onChange={() => toggleNotifPref(item.key)}
                  className="h-4 w-4 accent-[#9B82F3]"
                />
              </label>
            ))}
          </div>
        </section>
      </div>

      {/* Reset & Sign Out */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-3xl border border-[#EAE4DC] bg-white p-5 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
        {!isBackendConfigured ? (
          <button
            type="button"
            onClick={() => mutations.resetDemoData.mutate()}
            className="inline-flex items-center gap-1.5 rounded-full px-4 py-2 text-xs font-semibold text-[#6E6A75] hover:bg-[#FAF7F2] hover:text-[#18181F] dark:hover:bg-[#272733]"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Reset sample data</span>
          </button>
        ) : (
          <span className="text-xs text-[#6E6A75]">DuoDesk Cloud Sync enabled</span>
        )}

        <button
          type="button"
          onClick={logout}
          className="inline-flex items-center gap-1.5 rounded-full bg-[#F8D8E4]/75 px-5 py-2 text-xs font-semibold text-[#8E3B5B] hover:bg-[#F8D8E4]"
        >
          <LogOut className="h-3.5 w-3.5" />
          <span>Sign out</span>
        </button>
      </div>
    </div>
  );
}
