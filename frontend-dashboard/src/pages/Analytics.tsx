import { useEffect, useState } from 'react';
import { getAnalyticsSnapshot } from '../api/admin';
import { DistributionChart } from '../components/analytics/DistributionChart';
import { HealthSummary } from '../components/analytics/HealthSummary';
import { HistogramChart } from '../components/analytics/HistogramChart';
import { ThrottleTimeline } from '../components/analytics/ThrottleTimeline';
import { Card } from '../components/common/Card';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingSkeleton } from '../components/common/LoadingSkeleton';
import { useAppContext } from '../context/AppContext';
import { usePageTitle } from '../hooks/usePageTitle';
import type { AnalyticsSnapshot } from '../types';

export function Analytics() {
  usePageTitle('Analytics');
  const { state, metrics } = useAppContext();
  const [snapshot, setSnapshot] = useState<AnalyticsSnapshot | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const load = async () => {
      if (!state.authToken) return;
      setLoading(true);
      try {
        const response = await getAnalyticsSnapshot(state.apiBaseUrl, state.authToken);
        if (active) setSnapshot(response.data);
      } catch {
        if (active) setSnapshot(null);
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [state.apiBaseUrl, state.authToken]);

  const averageLatencyMs =
    state.requestHistory.length === 0 ? 0 : state.requestHistory.reduce((sum, entry) => sum + entry.latencyMs, 0) / state.requestHistory.length;

  if (metrics.total === 0) {
    return (
      <div className="grid gap-6">
        <Card className="p-5">
          <h2 className="font-display text-2xl text-white">Understanding Your Traffic Patterns</h2>
          <p className="mt-2 text-sm text-slate-300">
            Analytics turns raw request history into operational insight. Use these charts to understand whether your rate limits are protecting the system or over-throttling legitimate users.
          </p>
        </Card>
        <EmptyState title="No traffic data yet." description="Use the API Tester on the Dashboard to generate requests, then return here to see analytics." />
      </div>
    );
  }

  return (
    <div className="grid gap-6">
      <Card className="p-5">
        <h2 className="font-display text-2xl text-white">Understanding Your Traffic Patterns</h2>
        <p className="mt-2 text-sm text-slate-300">
          Analytics turns raw request history into operational insight. Use these charts to understand whether your rate limits are protecting the system or over-throttling legitimate users.
        </p>
      </Card>

      {loading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <LoadingSkeleton className="h-72" />
          <LoadingSkeleton className="h-72" />
          <LoadingSkeleton className="h-72" />
        </div>
      ) : snapshot ? (
        <Card className="p-5">
          <h3 className="font-display text-xl text-white">Backend Observability Snapshot</h3>
          <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
              <p className="text-sm text-slate-400">Allowed decisions</p>
              <p className="mt-2 font-mono text-2xl text-white">{snapshot.rateLimitStats.allowedRequests}</p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
              <p className="text-sm text-slate-400">Denied decisions</p>
              <p className="mt-2 font-mono text-2xl text-white">{snapshot.rateLimitStats.deniedRequests}</p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
              <p className="text-sm text-slate-400">Avg decision latency</p>
              <p className="mt-2 font-mono text-2xl text-white">{snapshot.rateLimitStats.averageDecisionLatencyMs.toFixed(1)} ms</p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4">
              <p className="text-sm text-slate-400">Avg request latency</p>
              <p className="mt-2 font-mono text-2xl text-white">{snapshot.usageMetrics.averageRequestLatencyMs.toFixed(1)} ms</p>
            </div>
          </div>
        </Card>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-3">
        <DistributionChart metrics={metrics} />
        <HistogramChart history={state.requestHistory} />
        <ThrottleTimeline history={state.requestHistory} />
      </div>

      <HealthSummary metrics={metrics} averageLatencyMs={averageLatencyMs} />
    </div>
  );
}
