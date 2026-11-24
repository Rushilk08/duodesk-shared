/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MotionConfig } from 'motion/react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ToastStack } from './components/ToastStack';
import { WorkspaceProvider, useWorkspace } from './contexts/WorkspaceContext';
import { DashboardLayout } from './layouts/DashboardLayout';
import { CalendarPage } from './pages/CalendarPage';
import { CreatePage } from './pages/CreatePage';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { MySpacePage } from './pages/MySpacePage';
import { ProfilePage } from './pages/ProfilePage';
import { SharedSpacePage } from './pages/SharedSpacePage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
});

function AppRoutes() {
  const { currentUserId } = useWorkspace();

  if (!currentUserId) {
    return (
      <>
        <LoginPage />
        <ToastStack />
      </>
    );
  }

  return (
    <DashboardLayout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/my-space" element={<MySpacePage />} />
        <Route path="/shared-space" element={<SharedSpacePage />} />
        <Route path="/calendar" element={<CalendarPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/create" element={<CreatePage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </DashboardLayout>
  );
}

export default function App() {
  return (
    <MotionConfig reducedMotion="user">
      <QueryClientProvider client={queryClient}>
        <WorkspaceProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </WorkspaceProvider>
      </QueryClientProvider>
    </MotionConfig>
  );
}
