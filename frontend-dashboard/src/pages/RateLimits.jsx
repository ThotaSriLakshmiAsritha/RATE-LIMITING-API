import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Table } from '../components/ui/Table'

const statusBadge = (status) => {
  if (status === 429) {
    return <Badge tone="warning">429</Badge>
  }

  if (status >= 400) {
    return <Badge tone="error">{status}</Badge>
  }

  return <Badge tone="success">{status}</Badge>
}

export function RateLimits({ latestRateLimit, history, onClearHistory }) {
  const columns = [
    {
      key: 'timestamp',
      label: 'Timestamp',
      render: (value) => new Date(value).toLocaleTimeString(),
    },
    {
      key: 'label',
      label: 'Request',
    },
    {
      key: 'status',
      label: 'Status',
      render: (_, row) => statusBadge(row.status),
    },
    {
      key: 'durationMs',
      label: 'Duration',
      render: (value) => `${value} ms`,
    },
    {
      key: 'rateLimit',
      label: 'Remaining',
      render: (_, row) => row.rateLimit?.remaining || '-',
    },
  ]

  return (
    <>
      <Card title="Latest Header Snapshot" subtitle="Direct view of X-RateLimit and Retry-After values.">
        {!latestRateLimit ? (
          <p className="text-sm text-ink-500">No headers captured yet. Trigger requests from Dashboard.</p>
        ) : (
          <div className="grid gap-3 text-sm text-ink-700 sm:grid-cols-2 lg:grid-cols-4">
            <p>Limit <strong>{latestRateLimit.limit || '-'}</strong></p>
            <p>Remaining <strong>{latestRateLimit.remaining || '-'}</strong></p>
            <p>Reset <strong>{latestRateLimit.reset || '-'}</strong></p>
            <p>Retry-After <strong>{latestRateLimit.retryAfter || '-'}</strong></p>
          </div>
        )}
      </Card>

      <Card
        title="Request Ledger"
        subtitle="Last 40 calls through this UI"
        actions={<Button variant="ghost" onClick={onClearHistory}>Clear</Button>}
      >
        <Table columns={columns} rows={history} emptyMessage="No traffic yet." />
      </Card>
    </>
  )
}
