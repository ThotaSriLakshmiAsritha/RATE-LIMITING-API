import { useEffect, useState } from 'react';
import {
  BarChart3,
  BookOpenText,
  ChevronLeft,
  ChevronRight,
  Gauge,
  LockKeyhole,
  LogOut,
  Settings,
  SlidersHorizontal,
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { useAppContext } from '../../context/AppContext';
import { formatCountdown } from '../../utils/formatters';

const allNavItems = [
  { to: '/', label: 'Dashboard', icon: Gauge },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/rate-limits', label: 'Rate Limits', icon: SlidersHorizontal },
  { to: '/settings', label: 'Settings', icon: Settings },
  { to: '/admin', label: 'Admin', icon: LockKeyhole, adminOnly: true },
];

function NavItems({ collapsed, mobile = false }: { collapsed: boolean; mobile?: boolean }) {
  const { state } = useAppContext();
  return (
    <>
      {allNavItems
        .filter((item) => !item.adminOnly || state.role === 'ADMIN')
        .map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            aria-label={label}
            className={({ isActive }) =>
              `flex items-center ${mobile ? 'justify-center px-2 py-3' : 'gap-3 px-3 py-3'} rounded-2xl text-sm transition ${
                isActive ? 'bg-sky-500/18 text-sky-200' : 'text-slate-400 hover:bg-slate-900 hover:text-slate-100'
              }`
            }
          >
            <Icon className="h-5 w-5 shrink-0" />
            {!collapsed && !mobile ? <span>{label}</span> : null}
          </NavLink>
        ))}
    </>
  );
}

export function Sidebar() {
  const { state, logout, setSidebarCollapsed } = useAppContext();
  const [remainingMs, setRemainingMs] = useState<number | null>(state.tokenExpiry ? state.tokenExpiry - Date.now() : null);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setRemainingMs(state.tokenExpiry ? state.tokenExpiry - Date.now() : null);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [state.tokenExpiry]);

  return (
    <>
      <aside className={`hidden border-r border-slate-800 bg-slate-950/95 p-4 sm:flex sm:flex-col ${state.sidebarCollapsed ? 'sm:w-24' : 'sm:w-72'}`}>
        <div className="flex items-start justify-between gap-3">
          <div className={state.sidebarCollapsed ? 'hidden' : 'block'}>
            <p className="text-[11px] uppercase tracking-[0.34em] text-slate-500">Rate Limiting Control Plane</p>
            <h2 className="mt-2 font-display text-2xl text-slate-50">Atlas Pulse</h2>
            <p className="mt-1 text-sm text-slate-400">Observable, testable, and tunable.</p>
          </div>
          <button
            onClick={() => setSidebarCollapsed(!state.sidebarCollapsed)}
            aria-label={state.sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            className="rounded-xl border border-slate-800 p-2 text-slate-300 hover:border-sky-500/50 hover:text-white"
          >
            {state.sidebarCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </button>
        </div>

        <nav className="mt-8 flex flex-1 flex-col gap-2">
          <NavItems collapsed={state.sidebarCollapsed} />
        </nav>

        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
          <div className={`flex ${state.sidebarCollapsed ? 'justify-center' : 'items-start justify-between gap-3'}`}>
            {!state.sidebarCollapsed ? (
              <div>
                <p className="font-semibold text-slate-100">{state.username ?? 'Guest'}</p>
                <p className="mt-1 text-sm text-slate-400">Token expires in {formatCountdown(remainingMs)}</p>
              </div>
            ) : null}
            <Badge tone={state.role === 'ADMIN' ? 'warning' : 'info'}>{state.role ?? 'USER'}</Badge>
          </div>
          <div className={`mt-4 grid gap-2 ${state.sidebarCollapsed ? 'justify-center' : ''}`}>
            <Button className="w-full" variant="secondary" onClick={() => window.open(`${state.apiBaseUrl}/swagger-ui.html`, '_blank')} aria-label="Open Swagger UI">
              <BookOpenText className="mr-2 h-4 w-4" />
              {!state.sidebarCollapsed ? 'Open Swagger UI' : 'Docs'}
            </Button>
            <Button className="w-full" variant="ghost" onClick={() => logout(true)} aria-label="Log out">
              <LogOut className="mr-2 h-4 w-4" />
              {!state.sidebarCollapsed ? 'Logout' : 'Out'}
            </Button>
          </div>
        </div>
      </aside>

      <nav className="fixed bottom-0 left-0 right-0 z-40 border-t border-slate-800 bg-slate-950/98 p-2 sm:hidden">
        <div className="grid grid-cols-5 gap-1">
          <NavItems collapsed mobile />
        </div>
      </nav>
    </>
  );
}
