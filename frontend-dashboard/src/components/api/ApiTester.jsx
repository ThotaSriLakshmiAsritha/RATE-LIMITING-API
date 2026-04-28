import { useState } from 'react'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'

const defaultProduct = {
  name: 'Load-Test Widget',
  description: 'Created from the dashboard tester',
  price: 29.99,
}

export function ApiTester({ requestApi, latestResponse }) {
  const [credentials, setCredentials] = useState({ username: 'demo', password: 'password' })

  const renderRateLimit = () => {
    if (!latestResponse?.rateLimit) {
      return <p className="text-sm text-ink-500">Run any endpoint to inspect rate-limit headers.</p>
    }

    const { limit, remaining, reset, retryAfter } = latestResponse.rateLimit

    return (
      <div className="grid gap-2 text-sm text-ink-700 sm:grid-cols-2">
        <p>Limit: <strong>{limit || '-'}</strong></p>
        <p>Remaining: <strong>{remaining || '-'}</strong></p>
        <p>Reset: <strong>{reset || '-'}</strong></p>
        <p>Retry-After: <strong>{retryAfter || '-'}</strong></p>
      </div>
    )
  }

  return (
    <Card title="API Tester" subtitle="Run real backend requests and inspect rate-limit behavior live.">
      <div className="grid gap-3 lg:grid-cols-[1.2fr_1fr]">
        <div className="space-y-3">
          <div className="flex flex-wrap gap-2">
            <Button disabled={requestApi.inFlight} onClick={requestApi.runPing}>GET /ping</Button>
            <Button disabled={requestApi.inFlight || !requestApi.token} onClick={requestApi.runGetProducts}>
              GET /api/v1/products
            </Button>
            <Button
              disabled={requestApi.inFlight || !requestApi.token}
              onClick={() => requestApi.runCreateProduct(defaultProduct)}
            >
              POST /api/v1/products
            </Button>
          </div>

          <div className="rounded-xl border border-ink-200/80 bg-white/70 p-3">
            <p className="mb-2 text-xs uppercase tracking-[0.2em] text-ink-500">Authentication</p>
            <div className="grid gap-2 sm:grid-cols-2">
              <input
                value={credentials.username}
                onChange={(event) => setCredentials((current) => ({ ...current, username: event.target.value }))}
                className="input"
                placeholder="Username"
              />
              <input
                type="password"
                value={credentials.password}
                onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))}
                className="input"
                placeholder="Password"
              />
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Button disabled={requestApi.inFlight} onClick={() => requestApi.runLogin(credentials)}>
                POST /api/auth/login
              </Button>
              {requestApi.token ? <Badge tone="success">Token loaded</Badge> : <Badge>No token</Badge>}
            </div>
            {!requestApi.token ? (
              <p className="mt-2 text-xs text-ink-500">Use the seeded demo account to unlock the protected product endpoints.</p>
            ) : null}
          </div>
        </div>

        <div className="rounded-xl border border-ink-200/80 bg-white/70 p-3">
          <p className="mb-2 text-xs uppercase tracking-[0.2em] text-ink-500">Rate-Limit Headers</p>
          {renderRateLimit()}
        </div>
      </div>
    </Card>
  )
}
