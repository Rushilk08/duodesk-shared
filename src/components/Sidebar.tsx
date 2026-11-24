import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import {
  ArrowLeftRight,
  CalendarDays,
  FolderHeart,
  Home,
  User,
  Users,
} from 'lucide-react';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskQueries } from '../hooks/useDuoDeskData';
import { UserId } from '../types/duodesk';
import { UserAvatar } from './UserAvatar';

export const CORE_NAV_ITEMS = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/my-space', label: 'My Space', icon: FolderHeart },
  { to: '/shared-space', label: 'Shared Space', icon: Users },
  { to: '/calendar', label: 'Calendar', icon: CalendarDays },
  { to: '/profile', label: 'Profile', icon: User },
];

export function Sidebar() {
  const { currentUserId, isBackendConfigured, switchUser } = useWorkspace();
  const { usersQuery } = useDuoDeskQueries();
  const location = useLocation();
  const navigate = useNavigate();

  const users = usersQuery.data;
  const currentUser = currentUserId && users ? users[currentUserId] : undefined;
  const otherUserId: UserId = currentUserId === 'rushil' ? 'alex' : 'rushil';
  const otherUser = !isBackendConfigured && users ? users[otherUserId] : undefined;

  return (
    <aside className="hidden h-screen w-[224px] shrink-0 flex-col justify-between border-r border-[#EAE4DC] bg-white/80 px-4 py-6 backdrop-blur-sm lg:sticky lg:top-0 lg:flex dark:border-[#2E2D3B] dark:bg-[#1B1B24]/90">
      <div>
        {/* Brand */}
        <button
          type="button"
          onClick={() => navigate('/')}
          className="flex items-center gap-2.5 px-2 text-left focus:outline-none"
        >
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-2xl bg-[#E6DFFB] text-[#5B469C] dark:bg-[#9B82F3] dark:text-[#16161D]">
            <svg viewBox="0 0 24 24" className="h-4.5 w-4.5" fill="none">
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
            <p className="font-display text-base font-bold tracking-tight text-[#18181F] dark:text-white">
              DuoDesk
            </p>
            <p className="text-[11px] text-[#6E6A75] dark:text-[#A19CA8]">
              Your space. Our progress.
            </p>
          </div>
        </button>

        {/* Nav Links */}
        <nav aria-label="Main navigation" className="mt-8 space-y-1.5">
          {CORE_NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const active =
              item.to === '/'
                ? location.pathname === '/'
                : location.pathname.startsWith(item.to);

            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={`relative flex items-center gap-3 rounded-full px-3.5 py-2.5 text-sm font-medium transition-colors ${
                  active
                    ? 'text-[#18181F] dark:text-white'
                    : 'text-[#6E6A75] hover:bg-[#F5F1EB] hover:text-[#18181F] dark:text-[#A19CA8] dark:hover:bg-[#272733] dark:hover:text-white'
                }`}
              >
                {active && (
                  <motion.span
                    layoutId="desktopNavPill"
                    transition={{ type: 'spring', stiffness: 380, damping: 32 }}
                    className="absolute inset-0 rounded-full bg-[#F8D8E4]/70 dark:bg-[#2D283E]"
                  />
                )}
                <Icon
                  className={`relative z-10 h-[18px] w-[18px] shrink-0 ${
                    active ? 'text-[#8E3B5B] dark:text-[#F8D8E4]' : ''
                  }`}
                />
                <span className="relative z-10 truncate">{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Bottom Quick Switcher / Active Profile */}
      <div className="space-y-2 rounded-3xl border border-[#EAE4DC] bg-[#FAF7F2] p-3 dark:border-[#2E2D3B] dark:bg-[#23222E]">
        <div className="flex items-center gap-2.5">
          <UserAvatar user={currentUser} size="sm" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold text-[#18181F] dark:text-white">
              {currentUser?.name ?? 'Member'}
            </p>
            <p className="truncate text-[11px] text-[#6E6A75] dark:text-[#A19CA8]">
              {isBackendConfigured
                ? currentUser?.role ?? 'DuoDesk Member'
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
            className="flex w-full items-center justify-between rounded-full bg-white px-3 py-1.5 text-[11px] font-semibold text-[#18181F] shadow-2xs transition-colors hover:bg-[#E6DFFB]/60 dark:bg-[#2E2D3B] dark:text-white dark:hover:bg-[#3A384A]"
          >
            <span>Switch to {otherUser.name}</span>
            <ArrowLeftRight className="h-3 w-3 text-[#9B82F3]" />
          </button>
        )}
      </div>
    </aside>
  );
}
