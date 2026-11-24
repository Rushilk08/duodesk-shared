import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'motion/react';
import { Bell, Check, Plus, Trash2 } from 'lucide-react';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskMutations, useDuoDeskQueries } from '../hooks/useDuoDeskData';
import { formatRelativeTime, getGreetingByHour } from '../utils/formatters';
import { UserAvatar } from './UserAvatar';

export function TopHeader() {
  const { currentUserId, setCreateDefaults } = useWorkspace();
  const { usersQuery, notificationsQuery } = useDuoDeskQueries();
  const mutations = useDuoDeskMutations();
  const location = useLocation();
  const navigate = useNavigate();

  const [notifOpen, setNotifOpen] = useState(false);

  const users = usersQuery.data;
  const currentUser = currentUserId && users ? users[currentUserId] : undefined;
  const notifications = notificationsQuery.data ?? [];
  const unreadCount = notifications.filter((n) => !n.read).length;

  const todayFormatted = new Date().toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });

  const isHome = location.pathname === '/';

  const routeTitle =
    location.pathname === '/my-space'
      ? 'My Space'
      : location.pathname === '/shared-space'
      ? 'Shared Space'
      : location.pathname === '/calendar'
      ? 'Calendar'
      : location.pathname === '/profile'
      ? 'Profile'
      : location.pathname === '/create'
      ? 'New Item'
      : 'Home';

  const handleOpenCreate = () => {
    setCreateDefaults({
      kind: 'task',
      visibility: location.pathname === '/shared-space' ? 'shared' : 'private',
    });
    navigate('/create');
  };

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between gap-4 bg-[#FAF7F2]/90 px-4 py-4 backdrop-blur-md sm:px-8 dark:bg-[#16161D]/90">
      {/* Left: Greeting + Date on Home, or Clean Page Heading */}
      <div className="min-w-0">
        {isHome ? (
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate('/profile')}
              className="rounded-full focus:outline-none lg:hidden"
              aria-label="Open profile"
            >
              <UserAvatar user={currentUser} size="md" />
            </button>
            <div className="min-w-0">
              <p className="font-mono text-[11px] font-medium text-[#6E6A75] dark:text-[#A19CA8]">
                {todayFormatted}
              </p>
              <p className="truncate font-display text-base font-bold text-[#18181F] sm:text-lg dark:text-white">
                {getGreetingByHour(currentUser?.name ?? 'Rushil')}!
              </p>
            </div>
          </div>
        ) : (
          <div>
            <p className="font-mono text-[11px] text-[#6E6A75] dark:text-[#A19CA8]">
              DuoDesk · {todayFormatted}
            </p>
            <p className="font-display text-lg font-bold text-[#18181F] dark:text-white">
              {routeTitle}
            </p>
          </div>
        )}
      </div>

      {/* Right: Notification Bell, Avatar, and '+' Create Pill */}
      <div className="flex shrink-0 items-center gap-2.5">
        <div className="relative">
          <button
            type="button"
            onClick={() => setNotifOpen((o) => !o)}
            className="relative flex h-10 w-10 items-center justify-center rounded-full border border-[#EAE4DC] bg-white text-[#18181F] shadow-2xs transition-colors hover:bg-[#F5F1EB] dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
            aria-label="Notifications"
          >
            <Bell className="h-4 w-4" />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-[#E684A7] px-1 font-mono text-[10px] font-bold text-white">
                {unreadCount}
              </span>
            )}
          </button>

          <AnimatePresence>
            {notifOpen && (
              <motion.div
                initial={{ opacity: 0, y: 8, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 8, scale: 0.96 }}
                transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
                className="absolute top-full right-0 z-50 mt-2 w-80 overflow-hidden rounded-3xl border border-[#EAE4DC] bg-white shadow-[0_18px_45px_rgba(24,24,31,0.12)] sm:w-96 dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
              >
                <div className="flex items-center justify-between border-b border-[#F2EDE6] px-5 py-3.5 dark:border-[#2E2D3B]">
                  <span className="font-display text-sm font-semibold text-[#18181F] dark:text-white">
                    Notifications
                  </span>
                  <div className="flex items-center gap-3">
                    {unreadCount > 0 && (
                      <button
                        type="button"
                        onClick={() => mutations.markAllNotificationsRead.mutate()}
                        className="text-xs font-semibold text-[#9B82F3] hover:underline"
                      >
                        Mark all read
                      </button>
                    )}
                    {notifications.length > 0 && (
                      <button
                        type="button"
                        onClick={() => mutations.clearNotifications.mutate()}
                        className="rounded-full p-1 text-[#6E6A75] hover:text-[#DC2626]"
                        title="Clear notifications"
                        aria-label="Clear notifications"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                </div>

                <div className="max-h-72 divide-y divide-[#F5F1EB] overflow-y-auto dark:divide-[#2E2D3B]">
                  {notifications.length === 0 ? (
                    <p className="px-5 py-8 text-center text-xs text-[#6E6A75]">
                      All caught up. No new alerts.
                    </p>
                  ) : (
                    notifications.map((n) => (
                      <div
                        key={n.id}
                        className={`flex items-start justify-between gap-3 px-5 py-3.5 transition-colors hover:bg-[#FAF7F2] dark:hover:bg-[#272733] ${
                          !n.read ? 'bg-[#E6DFFB]/25 dark:bg-[#9B82F3]/10' : ''
                        }`}
                      >
                        <button
                          type="button"
                          onClick={() => {
                            if (!n.read) mutations.markNotificationRead.mutate(n.id);
                            setNotifOpen(false);
                            const dest =
                              n.targetRoute === '/shared-space'
                                ? '/shared-space'
                                : n.targetRoute === '/calendar'
                                ? '/calendar'
                                : '/my-space';
                            navigate(dest);
                          }}
                          className="min-w-0 flex-1 text-left"
                        >
                          <p
                            className={`text-xs ${
                              n.read
                                ? 'font-medium text-[#6E6A75]'
                                : 'font-semibold text-[#18181F] dark:text-white'
                            }`}
                          >
                            {n.title}
                          </p>
                          <p className="mt-0.5 truncate text-[11px] text-[#6E6A75] dark:text-[#A19CA8]">
                            {n.message}
                          </p>
                          <p className="mt-1 font-mono text-[10px] text-[#9A94A3]">
                            {formatRelativeTime(n.createdAt)}
                          </p>
                        </button>
                        {!n.read && (
                          <button
                            type="button"
                            onClick={() => mutations.markNotificationRead.mutate(n.id)}
                            className="mt-0.5 rounded-full p-1 text-[#9B82F3] hover:bg-[#E6DFFB]/60"
                            aria-label="Mark as read"
                          >
                            <Check className="h-3.5 w-3.5" />
                          </button>
                        )}
                      </div>
                    ))
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <button
          type="button"
          onClick={() => navigate('/profile')}
          className="hidden rounded-full transition-transform hover:scale-105 lg:block"
          aria-label="Profile"
        >
          <UserAvatar user={currentUser} size="md" />
        </button>

        <button
          type="button"
          onClick={handleOpenCreate}
          className="inline-flex h-10 items-center gap-1.5 rounded-full bg-[#18181F] px-4 text-xs font-semibold whitespace-nowrap text-white shadow-sm transition-transform duration-150 hover:-translate-y-0.5 active:translate-y-0 dark:bg-[#9B82F3] dark:text-[#16161D]"
          aria-label="Create new item"
        >
          <Plus className="h-4 w-4" />
          <span className="hidden sm:inline">New</span>
        </button>
      </div>
    </header>
  );
}
