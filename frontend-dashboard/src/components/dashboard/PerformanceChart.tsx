import { ResponsiveContainer, CartesianGrid, Legend, Line, LineChart, Tooltip as ReTooltip, XAxis, YAxis } from 'recharts';
import type { RequestEntry } from '../../types';
import { Card } from '../common/Card';
import { EmptyState } from '../common/EmptyState';

export function PerformanceChart({ history }: { history: RequestEntry[] }) {
  const recent = [...history].reverse().slice(-40).map((item, index) => ({
    seq: index + 1,
    success: item.statusCode >= 200 && item.statusCode < 300 ? Math.round(item.latencyMs) : null,
    throttled: item.statusCode === 429 ? Math.round(item.latencyMs) : null,
    error: item.statusCode !== 429 && !(item.statusCode >= 200 && item.statusCode < 300) ? Math.round(item.latencyMs) : null,
  }));

  return (
    <Card className="p-5">
      <div className="mb-4">
        <h3 className="font-display text-xl text-white">Performance Line</h3>
        <p className="mt-1 text-sm text-slate-400">Response-speed shape over your most recent traffic window.</p>
      </div>
      {recent.length === 0 ? (
        <EmptyState title="No requests yet." description="Run a few API calls to render performance trends." />
      ) : (
        <div className="h-80">
          <ResponsiveContainer>
            <LineChart data={recent}>
              <CartesianGrid stroke="#263043" strokeDasharray="3 3" />
              <XAxis dataKey="seq" stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <ReTooltip />
              <Legend />
              <Line type="monotone" dataKey="success" name="Successful" stroke="#22c55e" strokeWidth={3} dot={false} />
              <Line type="monotone" dataKey="throttled" name="Throttled (429)" stroke="#f59e0b" strokeWidth={3} dot={false} />
              <Line type="monotone" dataKey="error" name="Errors" stroke="#94a3b8" strokeWidth={3} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}
