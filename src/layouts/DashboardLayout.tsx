import React from 'react';
import { useLocation } from 'react-router-dom';
import { motion } from 'motion/react';
import { MobileBottomNav } from '../components/MobileBottomNav';
import { Sidebar } from '../components/Sidebar';
import { ToastStack } from '../components/ToastStack';
import { TopHeader } from '../components/TopHeader';

export function DashboardLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();

  return (
    <div className="flex min-h-screen bg-[#FAF7F2] text-[#18181F] dark:bg-[#16161D] dark:text-[#F5F3EE]">
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col pb-20 lg:pb-6">
        <TopHeader />
        <main className="mx-auto w-full max-w-[1040px] flex-1 px-4 pt-2 pb-8 sm:px-8">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          >
            {children}
          </motion.div>
        </main>
      </div>

      <MobileBottomNav />
      <ToastStack />
    </div>
  );
}
