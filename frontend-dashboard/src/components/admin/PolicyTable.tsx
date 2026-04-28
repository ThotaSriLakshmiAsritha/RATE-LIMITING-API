import { PenSquare, Trash2 } from 'lucide-react';
import type { Policy } from '../../types';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { Card } from '../common/Card';

export function PolicyTable({
  policies,
  onEdit,
}: {
  policies: Policy[];
  onEdit: (policy: Policy) => void;
}) {
  return (
    <Card className="overflow-hidden">
      <div className="border-b border-slate-800 p-5">
        <h3 className="font-display text-xl text-white">Policy Management</h3>
        <p className="mt-1 text-sm text-slate-400">
          Policies define the rules. Each one combines a scope, a dimension, and a refill model.
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-950/80 text-left text-slate-400">
            <tr>
              {['ID', 'Name', 'Scope', 'Dimension', 'Requests/Min', 'Burst', 'Priority', 'Status', 'Actions'].map((header) => (
                <th key={header} className="px-4 py-3">{header}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {policies.map((policy, index) => (
              <tr key={policy.id} className={index % 2 === 0 ? 'bg-slate-900/40' : 'bg-slate-950/40'}>
                <td className="px-4 py-3 font-mono text-slate-300">{policy.id.slice(0, 8)}</td>
                <td className="px-4 py-3 text-slate-100">{policy.endpointPattern ?? 'Global policy'}</td>
                <td className="px-4 py-3 text-slate-300">{policy.scopeType}</td>
                <td className="px-4 py-3 text-slate-300">{policy.dimension ?? '—'}</td>
                <td className="px-4 py-3 font-mono text-slate-300">{policy.requestsPerMinute ?? '—'}</td>
                <td className="px-4 py-3 font-mono text-slate-300">{policy.burstCapacity ?? '—'}</td>
                <td className="px-4 py-3 font-mono text-slate-300">{policy.priority ?? '—'}</td>
                <td className="px-4 py-3">
                  <Badge tone="success">{policy.mode ?? 'ACTIVE'}</Badge>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <Button variant="secondary" onClick={() => onEdit(policy)} aria-label={`Edit policy ${policy.id}`}>
                      <PenSquare className="mr-2 h-4 w-4" />
                      Edit
                    </Button>
                    <Button variant="ghost" disabled aria-label="Delete policy unavailable">
                      <Trash2 className="mr-2 h-4 w-4" />
                      Delete unavailable
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
