import React, { useState, useEffect } from 'react';
import { 
  Zap, 
  Search, 
  Filter, 
  Activity, 
  ArrowRight,
  ShieldAlert,
  ShieldCheck
} from 'lucide-react';
import { cn } from '../lib/utils';

const generateEvent = () => {
  const endpoints = ['/api/v1/products', '/api/auth/login', '/api/v1/orders', '/ping'];
  const methods = ['GET', 'POST', 'PUT'];
  const decisions = ['ALLOWED', 'BLOCKED'];
  const randomEndpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const randomDecision = Math.random() > 0.8 ? 'BLOCKED' : 'ALLOWED';
  
  return {
    id: Math.random().toString(36).substr(2, 9),
    time: new Date().toLocaleTimeString(),
    endpoint: randomEndpoint,
    method: methods[Math.floor(Math.random() * methods.length)],
    decision: randomDecision,
    ip: `192.168.1.${Math.floor(Math.random() * 255)}`,
    latency: `${Math.floor(Math.random() * 100) + 10}ms`
  };
};

export const LiveTraffic = () => {
  const [events, setEvents] = useState<any[]>([]);

  useEffect(() => {
    // Simulate real-time feed
    const interval = setInterval(() => {
      setEvents(prev => [generateEvent(), ...prev].slice(0, 50));
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="space-y-8 animate-slide-up">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-primary rounded-xl shadow-lg shadow-primary/20">
            <Zap className="h-6 w-6 text-white animate-pulse" />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-card-foreground">Live Traffic</h1>
            <p className="text-muted-foreground mt-1">Real-time stream of incoming requests and decisions.</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 text-xs font-bold text-success bg-success/10 px-3 py-1.5 rounded-full ring-1 ring-success/20">
            <span className="h-2 w-2 rounded-full bg-success animate-ping" />
            Live Stream Connected
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-3 card flex flex-col h-[700px]">
          <div className="p-4 border-b border-border flex items-center justify-between bg-white/50">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground mr-4">
                <Filter className="h-4 w-4" /> Filter by:
              </div>
              <button className="badge bg-muted text-muted-foreground hover:bg-primary/10 hover:text-primary transition-all">All Traffic</button>
              <button className="badge bg-muted text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-all">Blocked Only</button>
            </div>
            <div className="text-xs text-muted-foreground font-medium">Showing last 50 events</div>
          </div>
          
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {events.length === 0 && (
              <div className="h-full flex flex-col items-center justify-center text-muted-foreground space-y-4">
                <Activity className="h-12 w-12 opacity-20 animate-spin" />
                <p>Waiting for incoming requests...</p>
              </div>
            )}
            {events.map((event) => (
              <div key={event.id} className="flex items-center justify-between p-4 rounded-xl border border-border bg-white hover:shadow-soft transition-all duration-300 group">
                <div className="flex items-center gap-6">
                  <div className={`p-2 rounded-lg ${event.decision === 'ALLOWED' ? 'bg-success/10' : 'bg-destructive/10'}`}>
                    {event.decision === 'ALLOWED' ? 
                      <ShieldCheck className="h-5 w-5 text-success" /> : 
                      <ShieldAlert className="h-5 w-5 text-destructive" />
                    }
                  </div>
                  <div className="flex flex-col">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold font-mono text-primary bg-primary/5 px-1.5 py-0.5 rounded">{event.method}</span>
                      <span className="font-semibold text-card-foreground">{event.endpoint}</span>
                    </div>
                    <span className="text-xs text-muted-foreground mt-1 font-mono">{event.ip}</span>
                  </div>
                </div>

                <div className="flex items-center gap-12">
                  <div className="flex flex-col items-end">
                    <span className={`text-sm font-bold ${event.decision === 'ALLOWED' ? 'text-success' : 'text-destructive'}`}>
                      {event.decision}
                    </span>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold tracking-wider">Decision</span>
                  </div>
                  <div className="flex flex-col items-end w-20">
                    <span className="text-sm font-mono font-medium text-card-foreground">{event.latency}</span>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold tracking-wider">Latency</span>
                  </div>
                  <div className="text-sm font-medium text-muted-foreground w-24 text-right">
                    {event.time}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-6">
          <div className="card p-6">
            <h3 className="text-sm font-bold text-muted-foreground uppercase tracking-wider mb-4">Traffic Statistics</h3>
            <div className="space-y-6">
              <div>
                <div className="flex items-center justify-between text-xs font-bold mb-2">
                  <span>ALLOWED</span>
                  <span className="text-success">92%</span>
                </div>
                <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
                  <div className="h-full bg-success rounded-full" style={{ width: '92%' }} />
                </div>
              </div>
              <div>
                <div className="flex items-center justify-between text-xs font-bold mb-2">
                  <span>BLOCKED</span>
                  <span className="text-destructive">8%</span>
                </div>
                <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
                  <div className="h-full bg-destructive rounded-full" style={{ width: '8%' }} />
                </div>
              </div>
            </div>
          </div>

          <div className="card p-6 bg-primary text-white">
            <h3 className="text-sm font-bold opacity-80 uppercase tracking-wider mb-4">Live Insights</h3>
            <p className="text-sm leading-relaxed">
              Detection engine identified a burst pattern from <span className="font-mono font-bold">192.168.1.42</span>. 
              Applying tactical policy <span className="underline font-semibold">"Aggressive IP Block"</span> for the next 5 minutes.
            </p>
            <button className="mt-4 w-full py-2 bg-white/20 hover:bg-white/30 rounded-lg text-xs font-bold transition-all backdrop-blur-sm">
              Review Anomaly
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
