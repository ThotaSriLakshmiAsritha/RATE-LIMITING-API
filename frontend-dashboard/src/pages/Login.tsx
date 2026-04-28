import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginRequest } from '../api/auth';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { useAppContext } from '../context/AppContext';
import { usePageTitle } from '../hooks/usePageTitle';

export function Login() {
  usePageTitle('Login');
  const { state, login } = useAppContext();
  const navigate = useNavigate();
  const [username, setUsername] = useState('demo');
  const [password, setPassword] = useState('password');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (state.authToken) {
      navigate('/', { replace: true });
    }
  }, [navigate, state.authToken]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await loginRequest(state.apiBaseUrl, username, password);
      login(response.data.access_token, username);
      navigate('/', { replace: true });
    } catch (caught: unknown) {
      const errorLike = caught as { status?: number; rateLimitHeaders?: { retryAfter?: number | null } };
      if (errorLike.status === 401) {
        setError('Invalid credentials');
      } else if (errorLike.status === 429) {
        setError(`Too many login attempts. Please wait ${errorLike.rateLimitHeaders?.retryAfter ?? 'a few'} seconds.`);
      } else {
        setError('Unable to reach the backend. Check the API base URL and backend status.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(59,130,246,0.18),_transparent_36%),linear-gradient(180deg,#0f1117_0%,#090b10_100%)] px-4">
      <Card className="w-full max-w-md p-8">
        <p className="text-xs uppercase tracking-[0.35em] text-slate-500">Rate Limiting Control Plane</p>
        <h1 className="mt-3 font-display text-4xl text-white">Atlas Pulse</h1>
        <p className="mt-3 text-sm text-slate-400">Distributed rate limiting, observable, testable, and tunable.</p>

        <form className="mt-8 grid gap-4" onSubmit={handleSubmit}>
          <label className="grid gap-2 text-sm text-slate-300">
            Username
            <input
              aria-label="Username"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-sky-400"
            />
          </label>
          <label className="grid gap-2 text-sm text-slate-300">
            Password
            <input
              aria-label="Password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-sky-400"
            />
          </label>
          {error ? <p className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{error}</p> : null}
          <Button type="submit" disabled={loading} aria-label="Sign in">
            {loading ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>

        <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-950/60 p-4">
          <Badge tone="info">Demo account</Badge>
          <p className="mt-2 text-sm text-slate-300">Use `demo / password` to explore the dashboard.</p>
        </div>
      </Card>
    </main>
  );
}
