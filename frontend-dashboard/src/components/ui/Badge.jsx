export function Badge({ children, tone = 'neutral' }) {
  const tones = {
    neutral: 'badge-neutral',
    success: 'badge-success',
    warning: 'badge-warning',
    error: 'badge-error',
  }

  return <span className={`badge ${tones[tone] || tones.neutral}`}>{children}</span>
}
