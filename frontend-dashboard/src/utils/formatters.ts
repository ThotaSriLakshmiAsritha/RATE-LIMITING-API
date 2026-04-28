export const formatTimestamp = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat([], {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        month: 'short',
        day: 'numeric',
      }).format(new Date(value))
    : 'Not available';

export const formatMs = (value?: number | null) => (value == null ? 'N/A' : `${Math.round(value)} ms`);

export const formatPercent = (value: number) => `${value.toFixed(1)}%`;

export const formatCountdown = (valueMs: number | null) => {
  if (!valueMs || valueMs <= 0) return '0m 0s';
  const totalSeconds = Math.floor(valueMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}m ${seconds}s`;
};
