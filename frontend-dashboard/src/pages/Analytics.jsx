import { LineChart } from '../components/charts/LineChart'
import { Card } from '../components/ui/Card'

export function Analytics({ metrics }) {
  return (
    <>
      <Card
        title="Traffic Behavior"
        subtitle="Synthetic quality index derived from response time and status."
      >
        <LineChart points={metrics.lineSeries} />
      </Card>

      <Card title="Interpretation" subtitle="How to read this chart for operations decisions.">
        <ul className="space-y-2 text-sm text-ink-700">
          <li>Higher trend means faster responses and healthier request cycles.</li>
          <li>Drops indicate slower endpoints or pressure from upstream dependencies.</li>
          <li>Spikes in throttled requests usually correlate with lower remaining quota.</li>
        </ul>
      </Card>
    </>
  )
}
