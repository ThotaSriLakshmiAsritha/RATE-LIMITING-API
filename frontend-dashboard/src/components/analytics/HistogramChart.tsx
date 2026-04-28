import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip as ReTooltip, XAxis, YAxis } from 'recharts';
import type { RequestEntry } from '../../types';
import { Card } from '../common/Card';

const bucketRules = [
  { label: '0-50ms', min: 0, max: 50, color: '#22c55e' },
  { label: '50-100ms', min: 50, max: 100, color: '#84cc16' },
  { label: '100-200ms', min: 100, max: 200, color: '#facc15' },
  { label: '200-500ms', min: 200, max: 500, color: '#fb923c' },
  { label: '500ms+', min: 500, max: Infinity, color: '#ef4444' },
];

export function HistogramChart({ history }: { history: RequestEntry[] }) {
  const data = bucketRules.map((bucket) => ({
    bucket: bucket.label,
    count: history.filter((entry) => entry.latencyMs >= bucket.min && entry.latencyMs < bucket.max).length,
    color: bucket.color,
  }));

  return (
    <Card className="min-w-0 p-5">
      <h3 className="font-display text-xl text-white">Response Time Distribution</h3>
      <p className="mt-1 text-sm text-slate-400">Buckets help non-technical viewers see what “fast” and “slow” look like at a glance.</p>
      <div className="mt-4 h-72 min-w-0">
        <ResponsiveContainer>
          <BarChart data={data}>
            <CartesianGrid stroke="#263043" strokeDasharray="3 3" />
            <XAxis dataKey="bucket" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" allowDecimals={false} />
            <ReTooltip />
            <Bar dataKey="count" radius={[8, 8, 0, 0]}>
              {data.map((entry) => (
                <Cell key={entry.bucket} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
