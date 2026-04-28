import { ChevronDown, ChevronUp } from 'lucide-react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';

export function ExplainerBanner({
  title,
  body,
  hidden,
  onToggle,
}: {
  title: string;
  body: string;
  hidden: boolean;
  onToggle: () => void;
}) {
  return (
    <Card className="overflow-hidden border-sky-500/20 bg-gradient-to-r from-sky-950/60 via-slate-900 to-emerald-950/40">
      <div className="flex items-start justify-between gap-4 p-5">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-sky-300">Explain It Plainly</p>
          <h2 className="mt-2 font-display text-2xl text-white">{title}</h2>
          {!hidden ? <p className="mt-3 max-w-4xl text-sm leading-7 text-slate-300">{body}</p> : null}
        </div>
        <Button variant="secondary" onClick={onToggle} aria-label={hidden ? 'Show explanation banner' : 'Hide explanation banner'}>
          {hidden ? <ChevronDown className="mr-2 h-4 w-4" /> : <ChevronUp className="mr-2 h-4 w-4" />}
          {hidden ? 'Show' : 'Hide'}
        </Button>
      </div>
    </Card>
  );
}
