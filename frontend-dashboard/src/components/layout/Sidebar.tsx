import React from 'react';
import { 
  LayoutDashboard, 
  BarChart3, 
  Settings, 
  Activity, 
  Box, 
  Lock, 
  ScrollText, 
  LogOut,
  ChevronRight
} from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '../../lib/utils';

const menuItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/' },
  { icon: BarChart3, label: 'Analytics', path: '/analytics' },
  { icon: Settings, label: 'Policies', path: '/policies' },
  { icon: Activity, label: 'Live Traffic', path: '/live' },
  { icon: Box, label: 'APIs', path: '/products' },
  { icon: Lock, label: 'Authentication', path: '/auth' },
  { icon: ScrollText, label: 'Logs', path: '/logs' },
];

export const Sidebar = () => {
  const location = useLocation();

  return (
    <aside className="fixed left-0 top-0 z-40 h-screen w-64 border-r border-border bg-white p-4 transition-transform lg:translate-x-0">
      <div className="flex h-full flex-col">
        <div className="mb-10 flex items-center gap-3 px-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary shadow-lg shadow-primary/20">
            <Activity className="h-6 w-6 text-white" />
          </div>
          <span className="text-xl font-bold tracking-tight text-card-foreground">RateGuard</span>
        </div>

        <nav className="flex-1 space-y-1">
          {menuItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.label}
                to={item.path}
                className={cn(
                  "group flex items-center justify-between rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
                  isActive 
                    ? "bg-primary text-white shadow-lg shadow-primary/10" 
                    : "text-muted-foreground hover:bg-muted hover:text-card-foreground"
                )}
              >
                <div className="flex items-center gap-3">
                  <item.icon className={cn("h-5 w-5", isActive ? "text-white" : "text-muted-foreground group-hover:text-primary")} />
                  {item.label}
                </div>
                {isActive && <ChevronRight className="h-4 w-4" />}
              </Link>
            );
          })}
        </nav>

        <div className="mt-auto border-t border-border pt-4">
          <div className="mb-4 flex items-center gap-3 px-2">
            <div className="h-10 w-10 overflow-hidden rounded-full bg-muted">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Admin" alt="User" className="h-full w-full object-cover" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-semibold text-card-foreground">Admin User</span>
              <span className="text-xs text-muted-foreground">admin@rateguard.io</span>
            </div>
          </div>
          <button className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-destructive hover:bg-destructive/10 transition-colors">
            <LogOut className="h-5 w-5" />
            Logout
          </button>
        </div>
      </div>
    </aside>
  );
};
