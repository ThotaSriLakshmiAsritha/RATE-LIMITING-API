import type { StreamEvent } from '../../types';
import { formatTimestamp } from '../../utils/formatters';
import { Badge } from '../common/Badge';
import { Card } from '../common/Card';

export function AuditTable({ events }: { events: StreamEvent[] }) {
  return (
    <Card className="overflow-hidden">
      <div className="border-b border-slate-800 p-5">
        <h3 className="font-display text-xl text-white">Realtime Policy Stream</h3>
        <p className="mt-1 text-sm text-slate-400">
          This backend exposes `/api/dashboard/stream` rather than the `/api/admin/audit` contract from your target spec, so this panel shows the live stream that is actually available today.
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-950/80 text-left text-slate-400">
            <tr>
              {['Time', 'Event Type', 'Payload Summary', 'Decision View'].map((header) => (
                <th key={header} className="px-4 py-3">{header}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {events.length === 0 ? (
              <tr>
                <td className="px-4 py-5 text-slate-400" colSpan={4}>
                  No stream events have arrived yet. Create or edit a policy to watch the backend broadcast an update.
                </td>
              </tr>
            ) : (
              events.map((event, index) => (
                <tr key={event.id} className={index % 2 === 0 ? 'bg-slate-900/40' : 'bg-slate-950/40'}>
                  <td className="px-4 py-3 text-slate-300">{formatTimestamp(event.emittedAt)}</td>
                  <td className="px-4 py-3 text-slate-100">{event.type}</td>
                  <td className="px-4 py-3 font-mono text-slate-300">{JSON.stringify(event.payload).slice(0, 88)}</td>
                  <td className="px-4 py-3">
                    <Badge tone="info">LIVE EVENT</Badge>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
