import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Tooltip } from '../common/Tooltip';

export function MetricCard({
  label,
  value,
  badge,
  tone,
  explanation,
}: {
  label: string;
  value: number;
  badge: string;
  tone: 'neutral' | 'success' | 'warning' | 'error';
  explanation: string;
}) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-slate-400">{label}</p>
          <p className="mt-4 font-mono text-4xl font-bold text-white">{value}</p>
        </div>
        <Tooltip text={explanation} />
      </div>
      <div className="mt-5">
        <Badge tone={tone}>{badge}</Badge>
      </div>
    </Card>
  );
}
