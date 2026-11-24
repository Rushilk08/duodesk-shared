import React, { useState } from 'react';
import { motion } from 'motion/react';
import { ArrowRight, KeyRound, Lock, UserPlus } from 'lucide-react';
import { UserAvatar } from '../components/UserAvatar';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskQueries } from '../hooks/useDuoDeskData';
import { UserId } from '../types/duodesk';

type AuthMode = 'login' | 'register' | 'reset';

export function LoginPage() {
  const {
    isBackendConfigured,
    pendingInviteCode,
    setCurrentUserId,
    login,
    register,
    requestPasswordReset,
    pushToast,
  } = useWorkspace();
  const { usersQuery } = useDuoDeskQueries();
  const users = usersQuery.data;

  const [mode, setMode] = useState<AuthMode>(pendingInviteCode ? 'register' : 'login');
  const [name, setName] = useState('');
  const [role, setRole] = useState('Member');
  const [inviteCode, setInviteCode] = useState(pendingInviteCode);
  const [email, setEmail] = useState(isBackendConfigured ? '' : 'rushil@duodesk.app');
  const [password, setPassword] = useState(isBackendConfigured ? '' : '••••••••');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleQuickLogin = (uid: UserId) => {
    setCurrentUserId(uid);
    pushToast({
      title: `Welcome back, ${uid === 'rushil' ? 'Rushil' : 'Alex'}`,
      description: 'Personal space & Shared Space ready.',
      tone: 'success',
    });
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    if (!isBackendConfigured) {
      const target: UserId = email.toLowerCase().includes('alex') ? 'alex' : 'rushil';
      handleQuickLogin(target);
      return;
    }

    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(email.trim(), password);
      } else if (mode === 'register') {
        await register({
          email: email.trim(),
          password,
          name: name.trim(),
          fullName: name.trim(),
          role: role.trim() || 'Member',
          inviteCode: inviteCode.trim() || undefined,
        });
      } else {
        await requestPasswordReset(email.trim());
        setMode('login');
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Authentication failed.';
      setErrorMsg(msg);
      pushToast({
        title: 'Authentication error',
        description: msg,
        tone: 'warning',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#FAF7F2] px-4 py-12 dark:bg-[#16161D]">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
        className="w-full max-w-md rounded-3xl border border-[#EAE4DC] bg-white p-7 shadow-[0_18px_50px_rgba(24,24,31,0.06)] sm:p-8 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
      >
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#E6DFFB] text-[#5B469C] dark:bg-[#9B82F3] dark:text-[#16161D]">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none">
              <rect
                x="4"
                y="5"
                width="7.5"
                height="14"
                rx="3"
                stroke="currentColor"
                strokeWidth="2"
              />
              <rect
                x="12.5"
                y="5"
                width="7.5"
                height="14"
                rx="3"
                fill="currentColor"
                fillOpacity="0.28"
                stroke="currentColor"
                strokeWidth="2"
              />
            </svg>
          </span>
          <div>
            <h1 className="font-display text-xl font-bold tracking-tight text-[#18181F] dark:text-white">
              DuoDesk
            </h1>
            <p className="text-xs text-[#6E6A75] dark:text-[#A19CA8]">
              Your space. Our progress.
            </p>
          </div>
        </div>

        {!isBackendConfigured ? (
          <>
            <p className="mt-6 text-sm text-[#6E6A75] dark:text-[#A19CA8]">
              Select a member profile to open their private space and your shared workspace.
            </p>

            <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {users &&
                (['rushil', 'alex'] as const).map((uid) => {
                  const member = users[uid];
                  if (!member) return null;
                  return (
                    <button
                      key={uid}
                      type="button"
                      onClick={() => handleQuickLogin(uid)}
                      className="group flex items-center gap-3 rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] p-3 text-left transition-all hover:border-[#9B82F3] hover:bg-white dark:border-[#343343] dark:bg-[#272733]"
                    >
                      <UserAvatar user={member} size="md" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                          {member.name}
                        </p>
                        <p className="truncate text-[11px] text-[#6E6A75]">
                          {uid === 'rushil' ? 'User A' : 'User B'}
                        </p>
                      </div>
                      <ArrowRight className="h-3.5 w-3.5 text-[#8E8996] transition-transform group-hover:translate-x-0.5 group-hover:text-[#9B82F3]" />
                    </button>
                  );
                })}
            </div>

            <div className="my-6 flex items-center gap-3 text-[11px] text-[#9A94A3]">
              <span className="h-px flex-1 bg-[#F2EDE6] dark:bg-[#2E2D3B]" />
              <span>or sign in with email</span>
              <span className="h-px flex-1 bg-[#F2EDE6] dark:bg-[#2E2D3B]" />
            </div>
          </>
        ) : (
          <div className="mt-6 mb-5 grid grid-cols-3 gap-1.5 rounded-full border border-[#EAE4DC] bg-[#FAF7F2] p-1 dark:border-[#343343] dark:bg-[#272733]">
            {(
              [
                { id: 'login', label: 'Sign in' },
                { id: 'register', label: 'Create account' },
                { id: 'reset', label: 'Reset' },
              ] as const
            ).map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  setMode(tab.id);
                  setErrorMsg('');
                }}
                className={`rounded-full py-1.5 text-xs font-semibold transition-colors ${
                  mode === tab.id
                    ? 'bg-[#18181F] text-white dark:bg-[#9B82F3] dark:text-[#16161D]'
                    : 'text-[#6E6A75] hover:text-[#18181F] dark:text-[#A19CA8]'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        )}

        <form onSubmit={handleFormSubmit} className="space-y-3.5">
          {isBackendConfigured && mode === 'register' && (
            <>
              <div className="grid grid-cols-2 gap-2.5">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Name</label>
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Rushil"
                    className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Role</label>
                  <input
                    type="text"
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    placeholder="Member"
                    className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                  />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">
                  Partner Invite Code (optional for User 1)
                </label>
                <input
                  type="text"
                  value={inviteCode}
                  onChange={(e) => setInviteCode(e.target.value)}
                  placeholder="DUO-XXXXXX"
                  className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 font-mono text-xs text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
                />
              </div>
            </>
          )}

          <div>
            <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
            />
          </div>

          {(!isBackendConfigured || mode !== 'reset') && (
            <div>
              <label className="mb-1 block text-xs font-semibold text-[#6E6A75]">Password</label>
              <input
                type="password"
                required
                minLength={isBackendConfigured ? 8 : 1}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
                className="w-full rounded-2xl border border-[#EAE4DC] bg-[#FAF7F2] px-4 py-2.5 text-sm text-[#18181F] focus:border-[#9B82F3] focus:bg-white focus:outline-none dark:border-[#343343] dark:bg-[#272733] dark:text-white"
              />
            </div>
          )}

          {errorMsg && (
            <p className="rounded-2xl bg-[#F8D8E4]/65 px-3.5 py-2 text-xs font-medium text-[#8E3B5B]">
              {errorMsg}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="flex w-full items-center justify-center gap-2 rounded-full bg-[#18181F] py-3 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60 dark:bg-[#9B82F3] dark:text-[#16161D]"
          >
            {mode === 'register' && isBackendConfigured ? (
              <UserPlus className="h-3.5 w-3.5" />
            ) : mode === 'reset' && isBackendConfigured ? (
              <KeyRound className="h-3.5 w-3.5" />
            ) : (
              <Lock className="h-3.5 w-3.5" />
            )}
            <span>
              {submitting
                ? 'Please wait...'
                : !isBackendConfigured || mode === 'login'
                ? 'Sign in'
                : mode === 'register'
                ? 'Create DuoDesk account'
                : 'Send reset link'}
            </span>
          </button>
        </form>
      </motion.div>
    </div>
  );
}
