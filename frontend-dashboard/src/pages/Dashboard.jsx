import { ApiTester } from '../components/api/ApiTester'
import { LineChart } from '../components/charts/LineChart'
import { Badge } from '../components/ui/Badge'
import { Card } from '../components/ui/Card'

function MetricCard({ label, value, tone = 'neutral', loading }) {
  return (
    <Card className="metric-card">
      <p className="text-xs uppercase tracking-[0.18em] text-ink-500">{label}</p>
      {loading ? (
        <div className="mt-3 h-8 w-16 animate-pulse rounded bg-ink-200/70" />
      ) : (
        <p className="mt-2 text-3xl font-bold text-ink-900">{value}</p>
      )}
      <div className="mt-3">
        <Badge tone={tone}>{tone.toUpperCase()}</Badge>
      </div>
    </Card>
  )
}

export function Dashboard({ metrics, requestApi }) {
  return (
    <>
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Total Requests"
          value={metrics.totals.total}
          tone="neutral"
          loading={metrics.health.loading}
        />
        <MetricCard
          label="Successful"
          value={metrics.totals.success}
          tone="success"
          loading={metrics.health.loading}
        />
        <MetricCard
          label="Throttled"
          value={metrics.totals.throttled}
          tone="warning"
          loading={metrics.health.loading}
        />
        <MetricCard
          label="Errors"
          value={metrics.totals.errors}
          tone="error"
          loading={metrics.health.loading}
        />
      </section>

      <Card title="Performance Line" subtitle="Response-speed shape over your most recent traffic window.">
        <LineChart points={metrics.lineSeries} />
      </Card>

      <ApiTester requestApi={requestApi} latestResponse={requestApi.latestResponse} />
    </>
  )
}
