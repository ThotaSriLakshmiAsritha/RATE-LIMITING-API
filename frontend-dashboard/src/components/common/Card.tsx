import { cn } from '../../lib/utils';

export function Card({ className, children }: { className?: string; children: React.ReactNode }) {
  return <section className={cn('rounded-2xl border border-slate-800 bg-slate-900/80 shadow-[0_16px_50px_rgba(0,0,0,0.28)]', className)}>{children}</section>;
}
