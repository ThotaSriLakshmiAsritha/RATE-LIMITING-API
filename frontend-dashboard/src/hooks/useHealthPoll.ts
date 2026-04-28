import { useEffect } from 'react';
import { ping } from '../api/health';
import { useAppContext } from '../context/AppContext';

export function useHealthPoll() {
  const { state, setBackendHealth } = useAppContext();

  useEffect(() => {
    let cancelled = false;

    const check = async () => {
      if (cancelled) return;
      setBackendHealth({
        status: 'checking',
        latencyMs: null,
        lastCheckedAt: null,
      });
      try {
        const start = performance.now();
        await ping(state.apiBaseUrl);
        if (!cancelled) {
          setBackendHealth({
            status: 'online',
            latencyMs: performance.now() - start,
            lastCheckedAt: new Date().toISOString(),
          });
        }
      } catch {
        if (!cancelled) {
          setBackendHealth({
            status: 'offline',
            latencyMs: null,
            lastCheckedAt: new Date().toISOString(),
          });
        }
      }
    };

    check();
    const interval = window.setInterval(check, 10000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [setBackendHealth, state.apiBaseUrl]);
}
