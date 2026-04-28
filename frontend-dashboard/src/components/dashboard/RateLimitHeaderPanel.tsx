import { Badge } from '../common/Badge';
import { Card } from '../common/Card';
import { EmptyState } from '../common/EmptyState';
import { formatMs, formatTimestamp } from '../../utils/formatters';
import type { RequestEntry } from '../../types';

const rows = [
  ['X-RateLimit-Limit', 'limit', 'Max requests allowed before the bucket is considered full.'],
  ['X-RateLimit-Remaining', 'remaining', 'How many tokens are still available right now.'],
  ['X-RateLimit-Reset', 'reset', 'Unix timestamp for when the bucket fully refills again.'],
  ['Retry-After', 'retryAfter', 'How many seconds the client should wait before retrying after a 429.'],
] as const;

export function RateLimitHeaderPanel({ latest }: { latest: RequestEntry | null }) {
  return (
    <Card className="p-5">
      <div className="mb-4">
        <h3 className="font-display text-xl text-white">Rate-Limit Headers</h3>
        <p className="mt-1 text-sm text-slate-400">Run any endpoint to inspect rate-limit headers.</p>
      </div>
      {!latest ? (
        <EmptyState title="No headers captured yet." description="Run any endpoint to inspect rate-limit headers." />
      ) : (
        <>
          <div className="grid gap-3 rounded-2xl border border-slate-800 bg-slate-950/50 p-4 text-sm text-slate-300">
            <div className="flex flex-wrap items-center gap-3">
              <span>
                Endpoint: <span className="font-mono text-slate-100">{latest.endpoint}</span>
              </span>
              <Badge tone={latest.statusCode >= 200 && latest.statusCode < 300 ? 'success' : latest.statusCode === 429 ? 'warning' : 'error'}>
                HTTP {latest.statusCode}
              </Badge>
            </div>
            <div className="grid gap-2 sm:grid-cols-3">
              <p>
                Response Time: <span className="font-mono text-slate-100">{formatMs(latest.latencyMs)}</span>
              </p>
              <p>
                Method: <span className="font-mono text-slate-100">{latest.method}</span>
              </p>
              <p>
                Timestamp: <span className="font-mono text-slate-100">{formatTimestamp(latest.timestamp)}</span>
              </p>
            </div>
          </div>
          <div className="mt-4 overflow-hidden rounded-2xl border border-slate-800">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-left text-slate-400">
                <tr>
                  <th className="px-4 py-3">Header</th>
                  <th className="px-4 py-3">Value</th>
                  <th className="px-4 py-3">What it means</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(([header, key, meaning], index) => (
                  <tr key={header} className={index % 2 === 0 ? 'bg-slate-900/50' : 'bg-slate-950/60'}>
                    <td className="px-4 py-3 font-mono text-slate-200">{header}</td>
                    <td className="px-4 py-3 font-mono text-white">{latest.rateLimitHeaders[key] ?? 'N/A'}</td>
                    <td className="px-4 py-3 text-slate-400">{meaning}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </Card>
  );
}
