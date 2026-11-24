import { useState } from 'react';
import { UserProfile } from '../types/duodesk';

interface UserAvatarProps {
  user?: UserProfile;
  size?: 'xs' | 'sm' | 'md' | 'lg';
  className?: string;
}

const SIZE_MAP = {
  xs: 'h-6 w-6 text-[10px]',
  sm: 'h-7 w-7 text-xs',
  md: 'h-9 w-9 text-xs',
  lg: 'h-12 w-12 text-sm',
};

export function UserAvatar({ user, size = 'md', className = '' }: UserAvatarProps) {
  const [imgError, setImgError] = useState(false);
  const name = user?.name ?? 'User';
  const initials = name
    .split(' ')
    .map((p) => p[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  if (!user?.avatarUrl || imgError) {
    return (
      <span
        className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white select-none ${SIZE_MAP[size]} ${className}`}
        style={{ backgroundColor: user?.accentHex ?? '#6C8FF5' }}
        aria-label={name}
      >
        {initials}
      </span>
    );
  }

  return (
    <img
      src={user.avatarUrl}
      alt={`${name} avatar`}
      referrerPolicy="no-referrer"
      onError={() => setImgError(true)}
      className={`shrink-0 rounded-full object-cover ring-1 ring-[#171B2C]/10 dark:ring-white/10 ${SIZE_MAP[size]} ${className}`}
    />
  );
}
