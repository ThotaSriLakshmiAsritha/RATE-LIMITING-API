import { CircleHelp } from 'lucide-react';

export function Tooltip({ text }: { text: string }) {
  return (
    <span className="group relative inline-flex items-center" aria-label={text}>
      <CircleHelp className="h-4 w-4 text-slate-400 transition group-hover:text-slate-100" />
      <span className="pointer-events-none absolute left-1/2 top-full z-20 mt-2 hidden w-64 -translate-x-1/2 rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-xs leading-relaxed text-slate-100 shadow-2xl group-hover:block">
        {text}
      </span>
    </span>
  );
}
