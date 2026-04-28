import React from 'react';
import { 
  ArrowUpRight, 
  ArrowDownRight, 
  CheckCircle2, 
  XCircle, 
  Activity, 
  ShieldCheck 
} from 'lucide-react';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  AreaChart,
  Area
} from 'recharts';
import { cn } from '../lib/utils';

const data = [
  { time: '12:00', allowed: 400, blocked: 24 },
  { time: '13:00', allowed: 300, blocked: 13 },
  { time: '14:00', allowed: 200, blocked: 980 },
  { time: '15:00', allowed: 278, blocked: 39 },
  { time: '16:00', allowed: 189, blocked: 48 },
  { time: '17:00', allowed: 239, blocked: 38 },
  { time: '18:00', allowed: 349, blocked: 43 },
];

const MetricCard = ({ title, value, change, trend, icon: Icon, color }: any) => (
  <div className="card p-6">
    <div className="flex items-center justify-between">
      <div className={cn("p-2 rounded-lg", color)}>
        <Icon className="h-6 w-6 text-white" />
      </div>
      <div className={cn(
        "flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-full",
        trend === 'up' ? "bg-success/10 text-success" : "bg-destructive/10 text-destructive"
      )}>
        {trend === 'up' ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
        {change}
      </div>
    </div>
    <div className="mt-4">
      <h3 className="text-sm font-medium text-muted-foreground">{title}</h3>
      <p className="text-2xl font-bold text-card-foreground mt-1">{value}</p>
    </div>
  </div>
);

export const Overview = () => {
  return (
    <div className="space-y-8 animate-slide-up">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-card-foreground">Overview</h1>
          <p className="text-muted-foreground mt-1">Real-time performance of your API rate limiting.</p>
        </div>
        <div className="flex gap-3">
          <button className="btn btn-ghost border border-border bg-white shadow-sm">Last 24 Hours</button>
          <button className="btn btn-primary shadow-lg shadow-primary/20">Download Report</button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard 
          title="Total Requests" 
          value="124,592" 
          change="+12.5%" 
          trend="up" 
          icon={Activity} 
          color="bg-primary"
        />
        <MetricCard 
          title="Allowed Traffic" 
          value="118,203" 
          change="+8.2%" 
          trend="up" 
          icon={CheckCircle2} 
          color="bg-success"
        />
        <MetricCard 
          title="Blocked Requests" 
          value="6,389" 
          change="-4.3%" 
          trend="down" 
          icon={XCircle} 
          color="bg-destructive"
        />
        <MetricCard 
          title="Success Rate" 
          value="94.8%" 
          change="+2.1%" 
          trend="up" 
          icon={ShieldCheck} 
          color="bg-secondary"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="card p-6 lg:col-span-2">
          <div className="flex items-center justify-between mb-8">
            <h3 className="text-lg font-bold">Traffic Distribution</h3>
            <div className="flex gap-4">
              <div className="flex items-center gap-2 text-xs font-medium">
                <span className="h-2 w-2 rounded-full bg-primary" /> Allowed
              </div>
              <div className="flex items-center gap-2 text-xs font-medium">
                <span className="h-2 w-2 rounded-full bg-destructive" /> Blocked
              </div>
            </div>
          </div>
          <div className="h-[350px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data}>
                <defs>
                  <linearGradient id="colorAllowed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#1D55F4" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#1D55F4" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis 
                  dataKey="time" 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#A2A7B2', fontSize: 12 }}
                  dy={10}
                />
                <YAxis 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#A2A7B2', fontSize: 12 }} 
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: '#fff', 
                    borderRadius: '12px', 
                    border: '1px solid #E2E8F0',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
                  }} 
                />
                <Area 
                  type="monotone" 
                  dataKey="allowed" 
                  stroke="#1D55F4" 
                  strokeWidth={3}
                  fillOpacity={1} 
                  fill="url(#colorAllowed)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="blocked" 
                  stroke="#EF4444" 
                  strokeWidth={3}
                  fillOpacity={0}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card p-6">
          <h3 className="text-lg font-bold mb-6">Endpoint Health</h3>
          <div className="space-y-6">
            {[
              { path: '/api/v1/products', status: 'Healthy', usage: 72, color: 'bg-success' },
              { path: '/api/auth/login', status: 'Warning', usage: 89, color: 'bg-warning' },
              { path: '/api/v1/orders', status: 'Healthy', usage: 45, color: 'bg-success' },
              { path: '/api/dashboard', status: 'Healthy', usage: 12, color: 'bg-success' },
            ].map((endpoint) => (
              <div key={endpoint.path} className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-card-foreground">{endpoint.path}</span>
                  <span className={cn("badge", 
                    endpoint.status === 'Healthy' ? "bg-success/10 text-success" : "bg-warning/10 text-warning"
                  )}>{endpoint.status}</span>
                </div>
                <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                  <div 
                    className={cn("h-full rounded-full transition-all duration-1000", endpoint.color)} 
                    style={{ width: `${endpoint.usage}%` }} 
                  />
                </div>
                <div className="text-[10px] text-muted-foreground text-right">{endpoint.usage}% capacity</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
