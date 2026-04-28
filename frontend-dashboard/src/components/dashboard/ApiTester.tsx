import { useEffect, useMemo, useState } from 'react';
import { LogIn, ShieldCheck, Zap } from 'lucide-react';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { Card } from '../common/Card';
import { Tooltip } from '../common/Tooltip';
import { useAppContext } from '../../context/AppContext';
import { useApi } from '../../hooks/useApi';
import { decodeJwt, isTokenExpired } from '../../utils/jwt';
import { formatCountdown } from '../../utils/formatters';

function EndpointButton({
  label,
  tooltip,
  className,
  onClick,
  disabled,
}: {
  label: string;
  tooltip: string;
  className: string;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <div className="flex items-center gap-2">
      <Button className={className} onClick={onClick} disabled={disabled} aria-label={label}>
        {label}
      </Button>
      <Tooltip text={tooltip} />
    </div>
  );
}

export function ApiTester() {
  const { state, login, addToast } = useAppContext();
  const { run, loading } = useApi();
  const [credentials, setCredentials] = useState({ username: 'demo', password: 'password' });
  const [tokenTick, setTokenTick] = useState(Date.now());

  useEffect(() => {
    const interval = window.setInterval(() => setTokenTick(Date.now()), 1000);
    return () => window.clearInterval(interval);
  }, []);

  const tokenStatus = useMemo(() => {
    if (!state.authToken) {
      return { label: 'No token', tone: 'neutral' as const, detail: 'Protected endpoints are locked until you log in.' };
    }
    if (isTokenExpired(state.authToken)) {
      return { label: 'Token expired', tone: 'error' as const, detail: 'The stored JWT is past its expiration time.' };
    }
    const payload = decodeJwt(state.authToken);
    const expiry = typeof payload.exp === 'number' ? payload.exp * 1000 - tokenTick : null;
    return { label: 'Token active', tone: 'success' as const, detail: `Expires in ${formatCountdown(expiry)}` };
  }, [state.authToken, tokenTick]);

  const handleLogin = async () => {
    try {
      const response = await run<{ access_token: string; token_type: string; expires_in: number }>({
        label: 'Login',
        path: '/api/auth/login',
        method: 'POST',
        body: credentials,
        toastOnSuccess: false,
      });
      login(response.access_token, credentials.username);
      addToast({
        tone: 'success',
        title: 'Login succeeded',
        description: 'JWT stored in localStorage and ready for protected requests.',
      });
    } catch (error: unknown) {
      const status = typeof error === 'object' && error && 'status' in error ? Number((error as { status?: number }).status) : undefined;
      const retryAfter =
        typeof error === 'object' && error && 'rateLimitHeaders' in error
          ? ((error as { rateLimitHeaders?: { retryAfter?: number | null } }).rateLimitHeaders?.retryAfter ?? null)
          : null;

      if (status === 401) {
        addToast({ tone: 'error', title: 'Invalid credentials', description: 'The demo credentials were not accepted.' });
      } else if (status === 429) {
        addToast({
          tone: 'warning',
          title: 'Too many login attempts',
          description: `Please wait ${retryAfter ?? 'a few'} seconds before retrying.`,
          countdownSeconds: retryAfter,
        });
      } else {
        addToast({ tone: 'error', title: 'Login failed', description: 'The backend did not issue a token.' });
      }
    }
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[1.35fr,0.95fr]">
      <Card className="p-5">
        <div className="mb-4">
          <h3 className="font-display text-xl text-white">API Tester</h3>
          <p className="mt-1 text-sm text-slate-400">
            Run real backend requests and inspect rate-limit behavior live. Watch what happens when you exceed the limit.
          </p>
        </div>
        <div className="rounded-2xl border border-sky-500/20 bg-sky-500/8 p-4 text-sm text-slate-300">
          Each button below fires a real HTTP request to the backend. The backend uses a token-bucket algorithm stored in Redis. Each user or IP gets a bucket of tokens that refills over time. When tokens run out, the server returns 429.
        </div>

        <div className="mt-5 flex flex-wrap gap-3">
          <EndpointButton
            label="GET /ping"
            className="bg-teal-500 text-slate-950 hover:bg-teal-400"
            tooltip="Health check endpoint. Always allowed and useful for proving the backend is alive."
            onClick={() => run({ label: 'Ping', path: '/ping', method: 'GET' })}
            disabled={loading}
          />
          <EndpointButton
            label="GET /api/v1/products"
            className="bg-sky-500 text-slate-950 hover:bg-sky-400"
            tooltip="Protected endpoint. Try clicking this rapidly to see the throttling headers change."
            onClick={() => run({ label: 'Get products', path: '/api/v1/products', method: 'GET', requiresAuth: true })}
            disabled={loading}
          />
          <EndpointButton
            label="POST /api/v1/products"
            className="bg-violet-500 text-white hover:bg-violet-400"
            tooltip="Protected write endpoint. POST paths often have tighter limits because they cost more."
            onClick={() =>
              run({
                label: 'Create product',
                path: '/api/v1/products',
                method: 'POST',
                requiresAuth: true,
                body: { name: 'Demo Product', price: 9.99 },
              })
            }
            disabled={loading}
          />
          {state.role === 'ADMIN' ? (
            <EndpointButton
              label="GET /api/v1/products/admin"
              className="bg-amber-500 text-slate-950 hover:bg-amber-400"
              tooltip="Admin-only endpoint. In this backend build it is role-protected and bypasses rate limiting."
              onClick={() => run({ label: 'Get admin products', path: '/api/v1/products/admin', method: 'GET', requiresAuth: true })}
              disabled={loading}
            />
          ) : null}
        </div>

        <Card className="mt-6 border-slate-800/90 bg-slate-950/50 p-5">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-emerald-300" />
            <h4 className="font-display text-lg text-white">Authentication</h4>
          </div>
          <div className="mt-4 grid gap-4 md:grid-cols-[1fr,1fr,auto]">
            <input
              aria-label="Authentication username"
              value={credentials.username}
              onChange={(event) => setCredentials((current) => ({ ...current, username: event.target.value }))}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-sky-400"
              placeholder="demo"
            />
            <input
              aria-label="Authentication password"
              type="password"
              value={credentials.password}
              onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-sky-400"
              placeholder="password"
            />
            <Button onClick={handleLogin} disabled={loading} aria-label="Sign in and store JWT token">
              <LogIn className="mr-2 h-4 w-4" />
              POST /api/auth/login
            </Button>
          </div>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <Badge tone={tokenStatus.tone}>{tokenStatus.label}</Badge>
            <span className="text-sm text-slate-400">{tokenStatus.detail}</span>
          </div>
          <p className="mt-3 text-sm text-slate-400">Use the seeded demo account to unlock the protected product endpoints.</p>
        </Card>
      </Card>

      <Card className="p-5">
        <div className="mb-4 flex items-center gap-2">
          <Zap className="h-4 w-4 text-amber-300" />
          <div>
            <h4 className="font-display text-lg text-white">Demo Guide</h4>
            <p className="text-sm text-slate-400">Walk a viewer through the rate-limit story in a repeatable order.</p>
          </div>
        </div>
        <ol className="grid gap-3 text-sm text-slate-300">
          <li>Step 1: Check the green backend badge in the top-right corner.</li>
          <li>Step 2: Click `POST /api/auth/login` to get a JWT token.</li>
          <li>Step 3: Click `GET /api/v1/products` and confirm you see a 200.</li>
          <li>Step 4: Click the same GET button rapidly 10 to 15 times.</li>
          <li>Step 5: Watch for the 429 response and the amber throttled card.</li>
          <li>Step 6: Note the `Retry-After` header and explain why it matters.</li>
          <li>Step 7: Open Rate Limits to inspect the full request ledger.</li>
          <li>Step 8: Open Analytics to explain the throttle-rate trend.</li>
        </ol>
      </Card>
    </div>
  );
}
