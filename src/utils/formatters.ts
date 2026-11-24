import { DocumentKind, Priority } from '../types/duodesk';

export function getTodayIso(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function addDaysIso(baseIso: string, days: number): string {
  const [y, m, d] = baseIso.split('-').map(Number);
  const dt = new Date(y, m - 1, d + days);
  const yy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, '0');
  const dd = String(dt.getDate()).padStart(2, '0');
  return `${yy}-${mm}-${dd}`;
}

export function formatShortDate(isoDate: string): string {
  if (!isoDate) return '';
  const [y, m, d] = isoDate.split('-').map(Number);
  const dt = new Date(y, m - 1, d);
  const today = getTodayIso();
  const tomorrow = addDaysIso(today, 1);
  const yesterday = addDaysIso(today, -1);

  if (isoDate === today) return 'Today';
  if (isoDate === tomorrow) return 'Tomorrow';
  if (isoDate === yesterday) return 'Yesterday';

  return dt.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });
}

export function formatFullDate(isoDate: string): string {
  if (!isoDate) return '';
  const [y, m, d] = isoDate.split('-').map(Number);
  const dt = new Date(y, m - 1, d);
  return dt.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function formatTime12h(time24?: string): string {
  if (!time24) return '';
  const [hh, mm] = time24.split(':').map(Number);
  if (Number.isNaN(hh) || Number.isNaN(mm)) return time24;
  const suffix = hh >= 12 ? 'PM' : 'AM';
  const h12 = hh % 12 === 0 ? 12 : hh % 12;
  return `${h12}:${String(mm).padStart(2, '0')} ${suffix}`;
}

export function formatRelativeTime(isoTimestamp: string): string {
  const dt = new Date(isoTimestamp);
  const diffMs = Date.now() - dt.getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days}d ago`;
  return dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(0)} KB`;
  const mb = kb / 1024;
  return `${mb.toFixed(1)} MB`;
}

export function isOverdueDate(isoDate: string, completed?: boolean): boolean {
  if (completed) return false;
  return isoDate < getTodayIso();
}

export function getPriorityMeta(priority: Priority): {
  label: string;
  textClass: string;
  dotColor: string;
} {
  switch (priority) {
    case 'high':
      return {
        label: 'High priority',
        textClass: 'text-[#DC2626] dark:text-[#F87171]',
        dotColor: '#DC2626',
      };
    case 'medium':
      return {
        label: 'Medium',
        textClass: 'text-[#D97706] dark:text-[#FBBF24]',
        dotColor: '#D97706',
      };
    case 'low':
      return {
        label: 'Low',
        textClass: 'text-[#5A627A] dark:text-[#9CA3AF]',
        dotColor: '#6C8FF5',
      };
  }
}

export function detectDocumentKind(fileName: string): {
  kind: DocumentKind;
  extension: string;
} {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
  if (ext === 'pdf') return { kind: 'pdf', extension: 'PDF' };
  if (ext === 'png' || ext === 'jpg' || ext === 'jpeg' || ext === 'webp') {
    return { kind: 'image', extension: ext.toUpperCase() };
  }
  return { kind: 'doc', extension: ext ? ext.toUpperCase() : 'DOC' };
}

export function getGreetingByHour(name: string): string {
  const hour = new Date().getHours();
  if (hour < 12) return `Good morning, ${name}`;
  if (hour < 17) return `Good afternoon, ${name}`;
  return `Good evening, ${name}`;
}
