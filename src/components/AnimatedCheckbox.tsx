import { motion } from 'motion/react';

interface AnimatedCheckboxProps {
  checked: boolean;
  onChange: () => void;
  label: string;
  accent?: 'lavender' | 'pink' | 'peach' | 'sage';
}

export function AnimatedCheckbox({
  checked,
  onChange,
  label,
  accent = 'lavender',
}: AnimatedCheckboxProps) {
  const checkedStyle =
    accent === 'pink'
      ? 'bg-[#E684A7] border-[#E684A7]'
      : accent === 'peach'
      ? 'bg-[#F2A36B] border-[#F2A36B]'
      : accent === 'sage'
      ? 'bg-[#5BB992] border-[#5BB992]'
      : 'bg-[#9B82F3] border-[#9B82F3]';

  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={checked}
      aria-label={label}
      onClick={(e) => {
        e.stopPropagation();
        onChange();
      }}
      className={`relative flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full border-2 transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9B82F3] focus-visible:ring-offset-2 dark:focus-visible:ring-offset-[#1F1F29] ${
        checked
          ? checkedStyle
          : 'border-[#D9D1C7] bg-white hover:border-[#9B82F3] dark:border-[#3A384A] dark:bg-[#272733] dark:hover:border-[#9B82F3]'
      }`}
    >
      <motion.svg
        viewBox="0 0 14 14"
        fill="none"
        className="h-3 w-3 text-white"
        initial={false}
        animate={{ scale: checked ? 1 : 0.65, opacity: checked ? 1 : 0 }}
        transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
      >
        <motion.path
          d="M2.8 7.2L5.6 10L11.2 4"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          initial={false}
          animate={{ pathLength: checked ? 1 : 0 }}
          transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
        />
      </motion.svg>
    </button>
  );
}
