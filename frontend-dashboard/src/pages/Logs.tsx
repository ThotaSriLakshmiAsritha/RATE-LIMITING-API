import React from 'react';
import { 
  Database, 
  Search, 
  Filter, 
  Calendar,
  ChevronLeft,
  ChevronRight,
  MoreHorizontal
} from 'lucide-react';

const logData = [
  { id: 101, time: '2026-04-28 20:01:01', endpoint: '/api/v1/products', method: 'GET', status: 200, decision: 'ALLOWED', latency: '45ms', user: 'guest_ip_12' },
  { id: 102, time: '2026-04-28 20:01:02', endpoint: '/api/v1/products', method: 'GET', status: 429, decision: 'BLOCKED', latency: '12ms', user: 'guest_ip_12' },
  { id: 103, time: '2026-04-28 20:01:05', endpoint: '/api/auth/login', method: 'POST', status: 200, decision: 'ALLOWED', latency: '180ms', user: 'demo' },
  { id: 104, time: '2026-04-28 20:01:10', endpoint: '/api/v1/orders', method: 'POST', status: 201, decision: 'ALLOWED', latency: '210ms', user: 'demo' },
  { id: 105, time: '2026-04-28 20:01:15', endpoint: '/api/v1/products', method: 'GET', status: 200, decision: 'ALLOWED', latency: '38ms', user: 'ip_192.168.1.5' },
];

export const Logs = () => {
  return (
    <div className="space-y-8 animate-slide-up">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-card-foreground">Audit Logs</h1>
          <p className="text-muted-foreground mt-1">Deep dive into every decision made by the rate limiter.</p>
        </div>
        <div className="flex gap-3">
          <button className="btn btn-ghost border border-border bg-white shadow-sm gap-2">
            <Calendar className="h-4 w-4" />
            Apr 28, 2026
          </button>
          <button className="btn btn-primary shadow-lg shadow-primary/20">Export CSV</button>
        </div>
      </div>

      <div className="card">
        <div className="p-6 border-b border-border flex flex-wrap gap-4 items-center justify-between bg-white/50">
          <div className="flex w-96 items-center gap-3 rounded-xl bg-muted px-4 py-2 ring-1 ring-border focus-within:ring-primary/20 transition-all">
            <Search className="h-4 w-4 text-muted-foreground" />
            <input type="text" placeholder="Filter by endpoint, IP, or user..." className="bg-transparent text-sm outline-none flex-1" />
          </div>
          <div className="flex gap-2">
            <button className="btn btn-ghost border border-border bg-white text-xs gap-2">
              <Filter className="h-3 w-3" /> Filters
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm border-separate border-spacing-0">
            <thead>
              <tr className="bg-muted/50 text-muted-foreground font-semibold">
                <th className="px-6 py-4 border-b border-border">Timestamp</th>
                <th className="px-6 py-4 border-b border-border">Endpoint</th>
                <th className="px-6 py-4 border-b border-border">Method</th>
                <th className="px-6 py-4 border-b border-border">Decision</th>
                <th className="px-6 py-4 border-b border-border">Status</th>
                <th className="px-6 py-4 border-b border-border">Latency</th>
                <th className="px-6 py-4 border-b border-border">User/Identity</th>
                <th className="px-6 py-4 border-b border-border text-right"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {logData.map((log) => (
                <tr key={log.id} className="hover:bg-muted/30 transition-colors group">
                  <td className="px-6 py-4 whitespace-nowrap text-muted-foreground font-mono text-xs">{log.time}</td>
                  <td className="px-6 py-4">
                    <span className="font-semibold text-card-foreground">{log.endpoint}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span className="bg-muted px-2 py-0.5 rounded text-[10px] font-bold">{log.method}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`font-bold text-xs ${log.decision === 'ALLOWED' ? 'text-success' : 'text-destructive'}`}>
                      {log.decision}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-bold">{log.status}</td>
                  <td className="px-6 py-4 font-mono text-muted-foreground">{log.latency}</td>
                  <td className="px-6 py-4 text-muted-foreground">{log.user}</td>
                  <td className="px-6 py-4 text-right">
                    <button className="p-1 rounded-md hover:bg-muted opacity-0 group-hover:opacity-100 transition-all">
                      <MoreHorizontal className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="p-6 border-t border-border flex items-center justify-between bg-muted/10">
          <span className="text-xs font-medium text-muted-foreground">Showing 1 to 5 of 1,240 logs</span>
          <div className="flex gap-2">
            <button className="p-2 rounded-lg border border-border bg-white hover:bg-muted transition-colors disabled:opacity-50" disabled>
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button className="p-2 rounded-lg border border-border bg-white hover:bg-muted transition-colors">
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
