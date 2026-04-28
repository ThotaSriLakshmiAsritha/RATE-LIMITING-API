import type { RateLimitHeaders } from '../../types';
import { Card } from '../common/Card';

export function TokenBucketDiagram({ headers }: { headers: RateLimitHeaders | null }) {
  const limit = headers?.limit ?? 10;
  const remaining = headers?.remaining ?? null;
  const retryAfter = headers?.retryAfter ?? null;
  const fillRatio = remaining != null && limit ? Math.max(0, Math.min(100, (remaining / limit) * 100)) : 50;
  const isEmpty = remaining === 0 || retryAfter != null;

  return (
    <Card className={`p-5 ${isEmpty ? 'border-rose-500/40' : 'border-slate-800'}`}>
      <div className="mb-4">
        <h3 className="font-display text-xl text-white">Token Bucket Story</h3>
        <p className="mt-1 text-sm text-slate-400">
          Tokens drip in over time, and each request spends one. Empty bucket means the backend sends HTTP 429.
        </p>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr,1.15fr]">
        <div className="relative mx-auto h-64 w-44 rounded-b-[3rem] rounded-t-[2rem] border-4 border-slate-600 bg-slate-950/70 p-3">
          <div className="absolute left-1/2 top-[-16px] h-6 w-14 -translate-x-1/2 rounded-full border border-slate-600 bg-slate-900" />
          <div className="absolute inset-x-3 bottom-3 overflow-hidden rounded-b-[2.2rem] rounded-t-[1.2rem] bg-slate-900">
            <div
              className={`absolute inset-x-0 bottom-0 bg-gradient-to-t ${isEmpty ? 'from-rose-500 to-amber-500' : 'from-sky-500 to-emerald-400'} transition-all duration-1000`}
              style={{ height: `${fillRatio}%` }}
            />
          </div>
          <div className="absolute -left-8 top-3 flex h-10 w-10 items-center justify-center rounded-full border border-emerald-400/30 bg-emerald-500/10 text-xl text-emerald-300 animate-bounce">
            +
          </div>
          <div className="absolute -right-8 bottom-5 flex h-10 w-10 items-center justify-center rounded-full border border-amber-400/30 bg-amber-500/10 text-xl text-amber-300 animate-pulse">
            -
          </div>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="rounded-full bg-slate-950/80 px-3 py-1 font-mono text-sm text-white">{isEmpty ? 'EMPTY' : 'FLOWING'}</span>
          </div>
        </div>
        <div className="grid gap-3 text-sm text-slate-300">
          <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
            <p className="text-slate-400">Limit</p>
            <p className="mt-2 font-mono text-2xl text-white">{headers?.limit ?? '—'} requests per minute</p>
          </div>
          <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
            <p className="text-slate-400">Remaining</p>
            <p className="mt-2 font-mono text-2xl text-white">{headers?.remaining ?? '—'} tokens</p>
          </div>
          <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
            <p className="text-slate-400">Reset</p>
            <p className="mt-2 font-mono text-2xl text-white">
              {headers?.reset ? `Resets at ${new Date(headers.reset * 1000).toLocaleTimeString()}` : 'Fire a request from the Dashboard to populate live values.'}
            </p>
          </div>
          {retryAfter != null ? <p className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-rose-200">Bucket is empty right now. The backend asked the client to retry after {retryAfter}s.</p> : null}
        </div>
      </div>
    </Card>
  );
}
