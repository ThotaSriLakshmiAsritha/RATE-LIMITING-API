import type { Metrics } from '../../types';
import { formatMs, formatPercent } from '../../utils/formatters';
import { Card } from '../common/Card';

export function HealthSummary({ metrics, averageLatencyMs }: { metrics: Metrics; averageLatencyMs: number }) {
  const throttleRate = metrics.total ? (metrics.throttled / metrics.total) * 100 : 0;
  const status =
    throttleRate <= 5
      ? { label: 'Healthy', color: 'text-emerald-300' }
      : throttleRate <= 20
        ? { label: 'Elevated throttling', color: 'text-amber-300' }
        : { label: 'Over-throttled', color: 'text-rose-300' };

  return (
    <Card className="p-5">
      <h3 className="font-display text-xl text-white">Interpretation Panel</h3>
      <p className="mt-3 text-sm leading-7 text-slate-300">
        Based on your current session:
        <br />
        Throttle Rate: <span className="font-mono text-white">{formatPercent(throttleRate)}</span> of requests were blocked (normal range: 0-5%).
        <br />
        Average Response Time: <span className="font-mono text-white">{formatMs(averageLatencyMs)}</span> (target: below 100ms).
        <br />
        Status: <span className={`font-semibold ${status.color}`}>{status.label}</span>
      </p>
    </Card>
  );
}
