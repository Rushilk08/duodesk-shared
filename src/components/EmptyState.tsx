import { motion } from 'motion/react';
import { LucideIcon, Plus } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction,
}: EmptyStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
      className="flex flex-col items-center justify-center rounded-3xl border border-[#EAE4DC] bg-white px-6 py-12 text-center shadow-[0_8px_24px_rgba(24,24,31,0.03)] dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
    >
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[#F8D8E4]/65 text-[#8E3B5B] dark:bg-[#2F2838] dark:text-[#F8D8E4]">
        <Icon className="h-5 w-5" />
      </div>
      <h3 className="font-display text-base font-semibold text-[#18181F] dark:text-[#F5F3EE]">
        {title}
      </h3>
      <p className="mt-1.5 max-w-xs text-sm leading-relaxed text-[#6E6A75] dark:text-[#A19CA8]">
        {description}
      </p>
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-5 inline-flex items-center gap-1.5 rounded-full bg-[#18181F] px-5 py-2.5 text-xs font-semibold text-white transition-transform duration-150 hover:-translate-y-0.5 active:translate-y-0 dark:bg-[#9B82F3] dark:text-[#16161D]"
        >
          <Plus className="h-3.5 w-3.5" />
          <span>{actionLabel}</span>
        </button>
      )}
    </motion.div>
  );
}
