import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { AppProvider } from './context/AppContext';
import { Sidebar } from './components/layout/Sidebar.tsx';
import { TopNav } from './components/layout/TopNav.tsx';
import { ProtectedRoute } from './components/layout/ProtectedRoute.tsx';
import { ToastViewport } from './components/common/Toast.tsx';
import { useHealthPoll } from './hooks/useHealthPoll';
import { Login } from './pages/Login.tsx';
import { NotFound } from './pages/NotFound.tsx';
import { Dashboard } from './pages/Dashboard.tsx';
import { Analytics } from './pages/Analytics.tsx';
import { RateLimits } from './pages/RateLimits.tsx';
import { Settings } from './pages/Settings.tsx';
import { Admin } from './pages/Admin.tsx';

const titleMap: Record<string, string> = {
  '/': 'Operational Dashboard',
  '/analytics': 'Analytics',
  '/rate-limits': 'Rate Limits',
  '/settings': 'Settings',
  '/admin': 'Admin Panel',
};

function Shell() {
  const location = useLocation();
  const navigate = useNavigate();
  useHealthPoll();

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if (event.ctrlKey && event.key.toLowerCase() === 'd') {
        event.preventDefault();
        navigate('/');
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [navigate]);

  return (
    <div className="flex min-h-screen bg-[radial-gradient(circle_at_top,_rgba(34,197,94,0.08),_transparent_28%),linear-gradient(180deg,#0f1117_0%,#090b10_100%)] text-slate-100">
      <Sidebar />
      <div className="flex min-h-screen flex-1 flex-col pb-20 sm:pb-0">
        <TopNav title={titleMap[location.pathname] ?? 'Atlas Pulse'} />
        <main className="flex-1 p-4 sm:p-6">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/rate-limits" element={<RateLimits />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/admin" element={<Admin />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>
      </div>
      <ToastViewport />
    </div>
  );
}

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/*" element={<Shell />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AppProvider>
  );
}
