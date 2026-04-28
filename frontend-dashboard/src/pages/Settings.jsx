import { Card } from '../components/ui/Card'

export function Settings({ baseUrl }) {
  return (
    <>
      <Card title="Backend Target" subtitle="Configure with VITE_API_BASE_URL in your frontend environment.">
        <div className="rounded-xl border border-ink-200/80 bg-white/70 p-3">
          <p className="text-xs uppercase tracking-[0.2em] text-ink-500">Current Base URL</p>
          <p className="mt-2 font-mono text-sm text-ink-800">{baseUrl}</p>
        </div>
      </Card>

      <Card title="Operator Notes" subtitle="Built for smoke validation and observability.">
        <ul className="space-y-2 text-sm text-ink-700">
          <li>Login first to issue authenticated product requests.</li>
          <li>Use Dashboard tester to intentionally trigger throttling and verify UX.</li>
          <li>Visit Rate Limits page to inspect live header behavior across requests.</li>
        </ul>
      </Card>
    </>
  )
}
