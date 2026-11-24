import { AnimatePresence, motion } from 'motion/react';
import { CheckCircle2, Info, X } from 'lucide-react';
import { useWorkspace } from '../contexts/WorkspaceContext';

export function ToastStack() {
  const { toasts, dismissToast } = useWorkspace();

  return (
    <div
      aria-live="polite"
      className="pointer-events-none fixed right-5 bottom-20 z-50 flex w-full max-w-xs flex-col gap-2 lg:bottom-6"
    >
      <AnimatePresence initial={false}>
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            initial={{ opacity: 0, y: 12, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.96 }}
            transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
            className="pointer-events-auto flex items-start gap-3 rounded-2xl border border-[#EAE4DC] bg-white px-4 py-3 shadow-[0_12px_30px_rgba(24,24,31,0.08)] dark:border-[#2E2D3B] dark:bg-[#23222E]"
          >
            {toast.tone === 'success' ? (
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-[#5BB992]" />
            ) : (
              <Info className="mt-0.5 h-4 w-4 shrink-0 text-[#9B82F3]" />
            )}
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold text-[#18181F] dark:text-[#F5F3EE]">
                {toast.title}
              </p>
              {toast.description && (
                <p className="mt-0.5 truncate text-[11px] text-[#6E6A75] dark:text-[#A19CA8]">
                  {toast.description}
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={() => dismissToast(toast.id)}
              className="rounded-full p-1 text-[#8E8996] hover:bg-[#F5F1EB] hover:text-[#18181F] dark:hover:bg-[#2E2D3B]"
              aria-label="Dismiss notification"
            >
              <X className="h-3 w-3" />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
