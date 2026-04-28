import { Activity } from 'lucide-react';
import { Badge } from '../common/Badge';
import { formatMs, formatTimestamp } from '../../utils/formatters';
import { useAppContext } from '../../context/AppContext';

export function TopNav({ title }: { title: string }) {
  const { state } = useAppContext();
  const tone =
    state.backendHealth.status === 'online'
      ? 'success'
      : state.backendHealth.status === 'offline'
        ? 'error'
        : 'warning';
  const label =
    state.backendHealth.status === 'online'
      ? 'Backend Online'
      : state.backendHealth.status === 'offline'
        ? 'Backend Offline'
        : 'Checking...';

  return (
    <header className="sticky top-0 z-30 border-b border-slate-800/80 bg-slate-950/75 backdrop-blur">
      <div className="flex items-center justify-between gap-4 px-4 py-4 sm:px-6">
        <div>
          <p className="text-xs uppercase tracking-[0.28em] text-slate-500">Operational View</p>
          <h1 className="font-display text-2xl text-slate-50">{title}</h1>
        </div>
        <div className="flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900/70 px-4 py-3">
          <Activity className="h-4 w-4 text-sky-300" />
          <div className="text-right">
            <Badge tone={tone}>{label}</Badge>
            <p className="mt-1 text-xs text-slate-400">
              {state.backendHealth.status === 'online'
                ? `Latency ${formatMs(state.backendHealth.latencyMs)}`
                : `Last check ${formatTimestamp(state.backendHealth.lastCheckedAt)}`}
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}
