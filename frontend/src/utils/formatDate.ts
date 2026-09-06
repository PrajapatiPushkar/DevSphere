export const formatRelativeTime = (isoString?: string): string => {
  if (!isoString) return 'Recently';

  const date = new Date(isoString);
  const time = date.getTime();
  if (isNaN(time)) return 'Recently';

  const now = Date.now();
  const diffSeconds = Math.floor((now - time) / 1000);

  if (diffSeconds < 30) return 'Just now';
  if (diffSeconds < 60) return `${diffSeconds}s ago`;

  const diffMinutes = Math.floor(diffSeconds / 60);
  if (diffMinutes < 60) return `${diffMinutes}m ago`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}h ago`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays}d ago`;

  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });
};
