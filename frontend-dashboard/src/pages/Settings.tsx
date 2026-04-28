import { useEffect, useState } from 'react';
import { actuatorHealth, fetchJwks, ping } from '../api/health';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { useAppContext } from '../context/AppContext';
import { usePageTitle } from '../hooks/usePageTitle';

const endpointRows = [
  ['/ping', 'GET', 'No', 'No', 'Health check'],
  ['/api/auth/login', 'POST', 'No', 'Yes', 'Get JWT token'],
  ['/api/v1/products', 'GET', 'Yes', 'Yes', 'List products'],
  ['/api/v1/products', 'POST', 'Yes', 'Yes', 'Create product'],
  ['/api/v1/products/admin', 'GET', 'Yes (ADMIN)', 'No in current build', 'Admin product list'],
  ['/api/dashboard/analytics', 'GET', 'Yes', 'Inherited app auth', 'Backend metrics snapshot'],
  ['/api/dashboard/policies', 'GET/POST', 'Yes', 'Inherited app auth', 'Policy management API'],
  ['/api/dashboard/stream', 'GET', 'Yes', 'Inherited app auth', 'Realtime dashboard event stream'],
  ['/actuator/health', 'GET', 'No', 'No', 'Spring health'],
  ['/actuator/prometheus', 'GET', 'No', 'No', 'Prometheus metrics'],
  ['/.well-known/jwks.json', 'GET', 'No', 'No', 'JWT public keys'],
];

export function Settings() {
  usePageTitle('Settings');
  const { state, setApiBaseUrl, addToast } = useAppContext();
  const [baseUrl, setBaseUrl] = useState(state.apiBaseUrl);
  const [connectionMessage, setConnectionMessage] = useState<string | null>(null);
  const [jwksCount, setJwksCount] = useState<number | null>(null);
  const [healthStatus, setHealthStatus] = useState<string | null>(null);
  const [authOpen, setAuthOpen] = useState(true);

  useEffect(() => {
    let active = true;
    const loadReference = async () => {
      try {
        const [jwksResponse, healthResponse] = await Promise.all([fetchJwks(state.apiBaseUrl), actuatorHealth(state.apiBaseUrl)]);
        if (active) {
          setJwksCount(jwksResponse.data.keys?.length ?? 0);
          setHealthStatus(healthResponse.data.status);
        }
      } catch {
        if (active) {
          setJwksCount(null);
          setHealthStatus(null);
        }
      }
    };
    loadReference();
    return () => {
      active = false;
    };
  }, [state.apiBaseUrl]);

  return (
    <div className="grid gap-6">
      <Card className="p-5">
        <h2 className="font-display text-2xl text-white">Configuration</h2>
        <p className="mt-2 text-sm text-slate-300">
          Configure the API base URL and view environment information. In production, this would point to your deployed backend. Locally, it points to localhost:8080.
        </p>
      </Card>

      <Card className="p-5">
        <h3 className="font-display text-xl text-white">API Base URL</h3>
        <div className="mt-4 flex flex-col gap-3 md:flex-row">
          <input
            aria-label="API base URL"
            value={baseUrl}
            onChange={(event) => setBaseUrl(event.target.value)}
            className="flex-1 rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-sky-400"
          />
          <Button
            onClick={() => {
              setApiBaseUrl(baseUrl.trim());
              addToast({ tone: 'success', title: 'Base URL saved', description: `All requests now target ${baseUrl.trim()}.` });
            }}
            aria-label="Save API base URL"
          >
            Save
          </Button>
          <Button
            variant="secondary"
            onClick={() => {
              setBaseUrl('http://localhost:8080');
              setApiBaseUrl('http://localhost:8080');
            }}
            aria-label="Reset API base URL"
          >
            Reset to default
          </Button>
        </div>
        <p className="mt-3 text-sm text-slate-400">
          All API requests in this dashboard will be sent to this URL. Change it if you deploy the backend to a different host.
        </p>
      </Card>

      <Card className="p-5">
        <h3 className="font-display text-xl text-white">Connection Test</h3>
        <div className="mt-4 flex flex-wrap gap-3">
          <Button
            onClick={async () => {
              try {
                const start = performance.now();
                await ping(baseUrl.trim());
                setConnectionMessage(`Connected. Backend responded in ${Math.round(performance.now() - start)}ms.`);
              } catch {
                setConnectionMessage('Cannot connect. Check the URL, backend status, or CORS configuration.');
              }
            }}
            aria-label="Test backend connection"
          >
            Test Connection
          </Button>
          <a className="inline-flex items-center rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-200 hover:border-sky-400" href={`${state.apiBaseUrl}/swagger-ui.html`} target="_blank" rel="noreferrer">
            Open Swagger UI {'->'}
          </a>
          <a className="inline-flex items-center rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-200 hover:border-sky-400" href={`${state.apiBaseUrl}/actuator/prometheus`} target="_blank" rel="noreferrer">
            View Prometheus Metrics {'->'}
          </a>
        </div>
        {connectionMessage ? <p className="mt-3 text-sm text-slate-300">{connectionMessage}</p> : null}
        <p className="mt-4 rounded-2xl border border-amber-500/25 bg-amber-500/10 p-4 text-sm text-amber-100">
          If requests fail with a browser CORS error, ensure the Spring Boot backend allows `http://localhost:5173`. This project already exposes that origin in `application.yml`.
        </p>
      </Card>

      <Card className="overflow-hidden">
        <div className="border-b border-slate-800 p-5">
          <h3 className="font-display text-xl text-white">Environment Reference</h3>
          <p className="mt-1 text-sm text-slate-400">
            Current runtime signals: JWKS keys <Badge tone="info">{jwksCount ?? 'unknown'}</Badge> and actuator status <Badge tone={healthStatus === 'UP' ? 'success' : 'warning'}>{healthStatus ?? 'unknown'}</Badge>.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-950/80 text-left text-slate-400">
              <tr>
                {['Endpoint', 'Method', 'Auth Required', 'Rate Limited', 'Description'].map((header) => (
                  <th key={header} className="px-4 py-3">{header}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {endpointRows.map((row, index) => (
                <tr key={row[0]} className={index % 2 === 0 ? 'bg-slate-900/40' : 'bg-slate-950/40'}>
                  {row.map((value) => (
                    <td key={value} className="px-4 py-3 text-slate-300">{value}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card className="p-5">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h3 className="font-display text-xl text-white">How Authentication Works</h3>
            <p className="mt-1 text-sm text-slate-400">Click to expand the browser-to-backend JWT flow.</p>
          </div>
          <Button variant="secondary" onClick={() => setAuthOpen(!authOpen)} aria-label="Toggle authentication explanation">
            {authOpen ? 'Hide' : 'Show'}
          </Button>
        </div>
        {authOpen ? (
          <>
            <p className="mt-4 text-sm leading-7 text-slate-300">
              When you log in, the backend validates your credentials and returns a signed JWT. Every subsequent request includes this token in the Authorization header. The backend validates the token signature using its public key material. Tokens expire after one hour, and the rate limiter can use the identity inside the token to apply per-user limits.
            </p>
            <div className="mt-5 grid gap-3 md:grid-cols-5">
              {['Login', 'JWT issued', 'Stored in browser', 'Sent with every request', 'Backend validates and rate-limits per user'].map((step) => (
                <div key={step} className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4 text-center text-sm text-slate-200">
                  {step}
                </div>
              ))}
            </div>
          </>
        ) : null}
      </Card>
    </div>
  );
}
