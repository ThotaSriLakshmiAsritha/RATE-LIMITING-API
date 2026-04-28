import type { RequestEntry } from '../../types';
import { Badge } from '../common/Badge';
import { Card } from '../common/Card';
import { EmptyState } from '../common/EmptyState';
import { formatMs, formatTimestamp } from '../../utils/formatters';

export function RequestLedger({ history }: { history: RequestEntry[] }) {
  if (history.length === 0) {
    return (
      <Card className="p-5">
        <h3 className="font-display text-xl text-white">Request History</h3>
        <p className="mt-1 text-sm text-slate-400">Every API call made this session, with full rate-limit context.</p>
        <div className="mt-4">
          <EmptyState title="No requests recorded yet." description="Run API calls from the Dashboard to populate the ledger." />
        </div>
      </Card>
    );
  }

  return (
    <Card className="overflow-hidden">
      <div className="border-b border-slate-800 p-5">
        <h3 className="font-display text-xl text-white">Request History</h3>
        <p className="mt-1 text-sm text-slate-400">Every API call made this session, with full rate-limit context.</p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-950/80 text-left text-slate-400">
            <tr>
              {['#', 'Timestamp', 'Endpoint', 'Method', 'Status', 'Latency', 'Limit', 'Remaining', 'Reset', 'Retry-After'].map((header) => (
                <th key={header} className="px-4 py-3">{header}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {history.map((entry, index) => {
              const rowClass =
                entry.statusCode >= 200 && entry.statusCode < 300
                  ? 'bg-emerald-500/6'
                  : entry.statusCode === 429
                    ? 'bg-amber-500/10'
                    : 'bg-rose-500/8';
              return (
                <tr key={entry.id} className={rowClass}>
                  <td className="px-4 py-3 font-mono text-slate-300">{history.length - index}</td>
                  <td className="px-4 py-3 text-slate-300">{formatTimestamp(entry.timestamp)}</td>
                  <td className="px-4 py-3 font-mono text-slate-100">{entry.endpoint}</td>
                  <td className="px-4 py-3 font-mono text-slate-300">{entry.method}</td>
                  <td className="px-4 py-3">
                    <Badge tone={entry.statusCode >= 200 && entry.statusCode < 300 ? 'success' : entry.statusCode === 429 ? 'warning' : 'error'}>
                      {entry.statusCode || 'NET'}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 font-mono text-slate-300">{formatMs(entry.latencyMs)}</td>
                  <td className="px-4 py-3 font-mono text-slate-300">{entry.rateLimitHeaders.limit ?? '—'}</td>
                  <td className="px-4 py-3 font-mono text-slate-300">{entry.rateLimitHeaders.remaining ?? '—'}</td>
                  <td className="px-4 py-3 font-mono text-slate-300">{entry.rateLimitHeaders.reset ?? '—'}</td>
                  <td className="px-4 py-3 font-mono text-slate-300">{entry.rateLimitHeaders.retryAfter ?? '—'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
