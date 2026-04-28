import { cn } from '../../lib/utils';

const toneMap = {
  neutral: 'bg-slate-800 text-slate-200 border-slate-700',
  success: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
  warning: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  error: 'bg-rose-500/15 text-rose-300 border-rose-500/30',
  info: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
};

export function Badge({
  tone = 'neutral',
  children,
  className,
}: {
  tone?: keyof typeof toneMap;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <span className={cn('inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.2em]', toneMap[tone], className)}>
      {children}
    </span>
  );
}
