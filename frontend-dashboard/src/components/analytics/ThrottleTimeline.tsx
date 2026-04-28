import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip as ReTooltip, XAxis, YAxis } from 'recharts';
import type { RequestEntry } from '../../types';
import { Card } from '../common/Card';

export function ThrottleTimeline({ history }: { history: RequestEntry[] }) {
  const sequence = [...history].reverse();
  const data = sequence.map((entry, index) => {
    const windowed = sequence.slice(Math.max(0, index - 4), index + 1);
    const throttled = windowed.filter((item) => item.statusCode === 429).length;
    return {
      seq: index + 1,
      rate: windowed.length ? Math.round((throttled / windowed.length) * 100) : 0,
      status: entry.statusCode,
    };
  });

  return (
    <Card className="min-w-0 p-5">
      <h3 className="font-display text-xl text-white">Throttle Rate Timeline</h3>
      <p className="mt-1 text-sm text-slate-400">A rolling view of how often the backend started saying “slow down.”</p>
      <div className="mt-4 h-72 min-w-0">
        <ResponsiveContainer>
          <AreaChart data={data}>
            <CartesianGrid stroke="#263043" strokeDasharray="3 3" />
            <XAxis dataKey="seq" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" unit="%" />
            <ReTooltip />
            <Area type="monotone" dataKey="rate" stroke="#f59e0b" fill="#f59e0b33" strokeWidth={3} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
