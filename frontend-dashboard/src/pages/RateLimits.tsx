import { useMemo, useState } from 'react';
import { Copy, Trash2 } from 'lucide-react';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { RequestLedger } from '../components/rate-limits/RequestLedger';
import { TokenBucketVisual } from '../components/rate-limits/TokenBucketVisual';
import { useAppContext } from '../context/AppContext';
import { usePageTitle } from '../hooks/usePageTitle';

export function RateLimits() {
  usePageTitle('Rate Limits');
  const { state, clearHistory, addToast } = useAppContext();
  const [confirming, setConfirming] = useState(false);

  const csv = useMemo(() => {
    const header = ['timestamp', 'endpoint', 'method', 'statusCode', 'latencyMs', 'limit', 'remaining', 'reset', 'retryAfter'];
    const rows = state.requestHistory.map((entry) => [
      entry.timestamp,
      entry.endpoint,
      entry.method,
      entry.statusCode,
      entry.latencyMs.toFixed(2),
      entry.rateLimitHeaders.limit ?? '',
      entry.rateLimitHeaders.remaining ?? '',
      entry.rateLimitHeaders.reset ?? '',
      entry.rateLimitHeaders.retryAfter ?? '',
    ]);
    return [header, ...rows].map((row) => row.join(',')).join('\n');
  }, [state.requestHistory]);

  return (
    <div className="grid gap-6">
      <Card className="p-5">
        <h2 className="font-display text-2xl text-white">Rate Limit Inspector</h2>
        <p className="mt-2 text-sm leading-7 text-slate-300">
          This page shows you the exact rate-limit headers returned by the backend for each request. The token-bucket algorithm gives each client a bucket of tokens. Each request costs one token. Tokens refill at a configured rate. When the bucket is empty, the server returns 429.
        </p>
      </Card>

      <TokenBucketVisual headers={state.latestHeaders} />

      <div className="flex flex-wrap justify-end gap-3">
        <Button
          variant="secondary"
          onClick={async () => {
            await navigator.clipboard.writeText(csv);
            addToast({ tone: 'success', title: 'Copied as CSV', description: 'The request ledger is now on your clipboard.' });
          }}
          aria-label="Copy request ledger as CSV"
        >
          <Copy className="mr-2 h-4 w-4" />
          Copy as CSV
        </Button>
        <Button variant="danger" onClick={() => setConfirming(true)} aria-label="Clear request history">
          <Trash2 className="mr-2 h-4 w-4" />
          Clear History
        </Button>
      </div>

      <RequestLedger history={state.requestHistory} />

      <ConfirmDialog
        open={confirming}
        title="Clear request history?"
        description="This resets the in-memory ledger, metrics, and latest captured headers for the current browser tab."
        onCancel={() => setConfirming(false)}
        onConfirm={() => {
          clearHistory();
          setConfirming(false);
        }}
      />
    </div>
  );
}
