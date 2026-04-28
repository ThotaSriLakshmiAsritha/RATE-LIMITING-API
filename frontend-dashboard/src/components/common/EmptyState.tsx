import { Info } from 'lucide-react';

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex min-h-48 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-700 bg-slate-950/40 px-6 py-10 text-center">
      <Info className="mb-3 h-8 w-8 text-sky-300" />
      <h3 className="font-display text-lg text-slate-100">{title}</h3>
      <p className="mt-2 max-w-xl text-sm text-slate-400">{description}</p>
    </div>
  );
}
