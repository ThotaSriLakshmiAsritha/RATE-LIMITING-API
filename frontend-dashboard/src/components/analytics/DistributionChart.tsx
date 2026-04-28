import { Pie, PieChart, Cell, Legend, ResponsiveContainer, Tooltip as ReTooltip } from 'recharts';
import type { Metrics } from '../../types';
import { Card } from '../common/Card';

export function DistributionChart({ metrics }: { metrics: Metrics }) {
  const data = [
    { name: 'Successful', value: metrics.successful, color: '#22c55e' },
    { name: 'Throttled', value: metrics.throttled, color: '#f59e0b' },
    { name: 'Errors', value: metrics.errors, color: '#ef4444' },
  ];

  return (
    <Card className="p-5">
      <h3 className="font-display text-xl text-white">Request Outcome Distribution</h3>
      <p className="mt-1 text-sm text-slate-400">See how much of the current session was allowed, throttled, or failed for other reasons.</p>
      <div className="mt-4 h-72">
        <ResponsiveContainer>
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" innerRadius={70} outerRadius={100} label>
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.color} />
              ))}
            </Pie>
            <ReTooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <p className="text-center font-mono text-lg text-slate-100">{metrics.total} total requests</p>
    </Card>
  );
}
