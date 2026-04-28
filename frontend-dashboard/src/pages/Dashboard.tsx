import { useMemo, useState } from 'react';
import { ApiTester } from '../components/dashboard/ApiTester';
import { ExplainerBanner } from '../components/dashboard/ExplainerBanner';
import { MetricCard } from '../components/dashboard/MetricCard';
import { PerformanceChart } from '../components/dashboard/PerformanceChart';
import { RateLimitHeaderPanel } from '../components/dashboard/RateLimitHeaderPanel';
import { useAppContext } from '../context/AppContext';
import { usePageTitle } from '../hooks/usePageTitle';

export function Dashboard() {
  usePageTitle('Dashboard');
  const { state, metrics, explainerHidden, setExplainerHidden } = useAppContext();
  const latest = state.requestHistory[0] ?? null;
  const [guideHidden, setGuideHidden] = useState(false);

  const cards = useMemo(
    () => [
      {
        label: 'Total Requests',
        value: metrics.total,
        badge: 'SESSION',
        tone: 'neutral' as const,
        explanation: 'Total number of API requests made through the tester in this session.',
      },
      {
        label: 'Successful',
        value: metrics.successful,
        badge: 'SUCCESS',
        tone: 'success' as const,
        explanation: 'Requests that were allowed through because the bucket still had tokens available.',
      },
      {
        label: 'Throttled',
        value: metrics.throttled,
        badge: 'THROTTLED',
        tone: 'warning' as const,
        explanation: 'Requests blocked by the rate limiter. The server responds with HTTP 429 and often includes Retry-After.',
      },
      {
        label: 'Errors',
        value: metrics.errors,
        badge: 'ERROR',
        tone: 'error' as const,
        explanation: 'Requests that failed due to auth, server, or network issues rather than rate limiting.',
      },
    ],
    [metrics],
  );

  return (
    <div className="grid gap-6">
      <ExplainerBanner
        title="What is Rate Limiting?"
        body="Rate limiting controls how many API requests a client can make in a given time window. When a client exceeds the limit, the server returns HTTP 429 and tells the client how long to wait before retrying. This dashboard shows that behavior happening in real time."
        hidden={explainerHidden}
        onToggle={() => setExplainerHidden(!explainerHidden)}
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {cards.map((card) => (
          <MetricCard key={card.label} {...card} />
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.35fr,0.95fr]">
        <PerformanceChart history={state.requestHistory} />
        <RateLimitHeaderPanel latest={latest} />
      </div>

      <ApiTester />

      {!guideHidden ? (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h3 className="font-display text-xl text-white">Why this matters</h3>
              <p className="mt-2 max-w-4xl text-sm leading-7 text-slate-300">
                A good rate limiter protects system health without blocking normal users. Use the counters, chart, and headers together: the counters show outcomes, the chart shows timing patterns, and the header panel shows the bucket values that explain those outcomes.
              </p>
            </div>
            <button className="text-sm text-slate-400 hover:text-white" onClick={() => setGuideHidden(true)} aria-label="Hide explanation section">
              Hide
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
