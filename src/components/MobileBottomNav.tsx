import { NavLink, useLocation } from 'react-router-dom';
import { motion } from 'motion/react';
import { CORE_NAV_ITEMS } from './Sidebar';

export function MobileBottomNav() {
  const location = useLocation();

  return (
    <nav
      aria-label="Mobile bottom navigation"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-[#EAE4DC] bg-white/95 px-2 py-2 backdrop-blur-md lg:hidden dark:border-[#2E2D3B] dark:bg-[#1B1B24]/95"
    >
      <div className="mx-auto flex max-w-md items-center justify-around">
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
              className={`relative flex flex-col items-center gap-1 rounded-2xl px-3 py-1.5 text-[11px] font-medium transition-colors ${
                active
                  ? 'text-[#18181F] dark:text-white'
                  : 'text-[#6E6A75] dark:text-[#A19CA8]'
              }`}
            >
              {active && (
                <motion.span
                  layoutId="mobileNavPill"
                  transition={{ type: 'spring', stiffness: 380, damping: 32 }}
                  className="absolute inset-0 rounded-2xl bg-[#F8D8E4]/70 dark:bg-[#2D283E]"
                />
              )}
              <Icon
                className={`relative z-10 h-4 w-4 ${
                  active ? 'text-[#8E3B5B] dark:text-[#F8D8E4]' : ''
                }`}
              />
              <span className="relative z-10 whitespace-nowrap">
                {item.label === 'Shared Space' ? 'Shared' : item.label}
              </span>
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
}
