import { useEffect, useState } from 'react';
import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react';
import { useAppContext } from '../../context/AppContext';
import { Badge } from './Badge';

const toneIcon = {
  success: CheckCircle2,
  warning: AlertTriangle,
  error: XCircle,
  info: Info,
};

export function ToastViewport() {
  const { state, removeToast } = useAppContext();

  return (
    <div className="fixed right-4 top-4 z-[60] flex w-[min(92vw,26rem)] flex-col gap-3">
      {state.toasts.map((toast) => (
        <ToastCard key={toast.id} {...toast} onClose={() => removeToast(toast.id)} />
      ))}
    </div>
  );
}

function ToastCard({
  title,
  description,
  tone,
  countdownSeconds,
  onClose,
}: {
  title: string;
  description: string;
  tone: 'success' | 'warning' | 'error' | 'info';
  countdownSeconds?: number | null;
  onClose: () => void;
}) {
  const Icon = toneIcon[tone];
  const [remaining, setRemaining] = useState(countdownSeconds ?? 0);

  useEffect(() => {
    if (!countdownSeconds) return;
    const interval = window.setInterval(() => setRemaining((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(interval);
  }, [countdownSeconds]);

  return (
    <div role="alert" className="rounded-2xl border border-slate-700 bg-slate-900/95 p-4 shadow-2xl backdrop-blur">
      <div className="flex items-start gap-3">
        <Icon className="mt-0.5 h-5 w-5 text-slate-100" />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="font-semibold text-slate-100">{title}</p>
            <Badge tone={tone === 'warning' ? 'warning' : tone === 'success' ? 'success' : tone === 'error' ? 'error' : 'info'}>
              {tone}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-slate-300">{description}</p>
          {countdownSeconds ? <p className="mt-2 text-xs text-amber-300">Retry timer: {remaining}s</p> : null}
        </div>
        <button onClick={onClose} className="text-sm text-slate-400 hover:text-slate-100" aria-label="Dismiss notification">
          Close
        </button>
      </div>
    </div>
  );
}
