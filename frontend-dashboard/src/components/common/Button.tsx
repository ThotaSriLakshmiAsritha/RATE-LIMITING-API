import { cn } from '../../lib/utils';

export function Button({
  className,
  variant = 'primary',
  children,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
}) {
  const variantClass =
    variant === 'primary'
      ? 'bg-emerald-500 text-slate-950 hover:bg-emerald-400'
      : variant === 'danger'
        ? 'bg-rose-500 text-white hover:bg-rose-400'
        : variant === 'ghost'
          ? 'bg-slate-800/50 text-slate-200 hover:bg-slate-700/70'
          : 'border border-slate-700 bg-slate-900 text-slate-200 hover:border-sky-500/50';

  return (
    <button
      className={cn(
        'inline-flex items-center justify-center rounded-xl px-4 py-2 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-sky-400 disabled:cursor-not-allowed disabled:opacity-50',
        variantClass,
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
