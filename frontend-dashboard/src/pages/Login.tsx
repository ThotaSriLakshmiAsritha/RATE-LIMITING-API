import React, { useState } from 'react';
import { Shield, Lock, User, ArrowRight, Activity } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const Login = () => {
  const [username, setUsername] = useState('demo');
  const [password, setPassword] = useState('password');
  const navigate = useNavigate();

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    // Simulate login and redirect to dashboard
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6 relative overflow-hidden">
      {/* Background Orbs */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/5 rounded-full blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-secondary/5 rounded-full blur-[120px]" />

      <div className="w-full max-w-md animate-slide-up">
        <div className="text-center mb-8">
          <div className="inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-primary shadow-2xl shadow-primary/30 mb-6 group transition-transform hover:scale-110">
            <Shield className="h-8 w-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight text-card-foreground">Welcome Back</h1>
          <p className="text-muted-foreground mt-2">Sign in to manage your API rate limits.</p>
        </div>

        <div className="card p-8 bg-white/80 backdrop-blur-xl">
          <form className="space-y-6" onSubmit={handleLogin}>
            <div className="space-y-2">
              <label className="text-sm font-semibold text-muted-foreground">Username</label>
              <div className="relative group">
                <div className="absolute left-3 top-3.5 text-muted-foreground transition-colors group-focus-within:text-primary">
                  <User className="h-5 w-5" />
                </div>
                <input 
                  type="text" 
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 rounded-xl border border-border bg-white outline-none focus:ring-2 focus:ring-primary/20 transition-all font-medium"
                  placeholder="Enter your username"
                />
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <label className="text-sm font-semibold text-muted-foreground">Password</label>
                <a href="#" className="text-xs font-bold text-primary hover:underline">Forgot?</a>
              </div>
              <div className="relative group">
                <div className="absolute left-3 top-3.5 text-muted-foreground transition-colors group-focus-within:text-primary">
                  <Lock className="h-5 w-5" />
                </div>
                <input 
                  type="password" 
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 rounded-xl border border-border bg-white outline-none focus:ring-2 focus:ring-primary/20 transition-all font-medium"
                  placeholder="Enter your password"
                />
              </div>
            </div>

            <button type="submit" className="w-full btn btn-primary py-4 text-base font-bold shadow-xl shadow-primary/25 rounded-xl group">
              Sign In
              <ArrowRight className="h-5 w-5 ml-2 transition-transform group-hover:translate-x-1" />
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-border flex items-center justify-center gap-2">
            <span className="text-xs font-bold text-muted-foreground uppercase tracking-widest">System Status</span>
            <div className="flex items-center gap-1.5 rounded-full bg-success/10 px-2.5 py-1 text-[10px] font-bold text-success ring-1 ring-success/20">
              <div className="h-1 w-1 rounded-full bg-success animate-pulse" />
              Healthy
            </div>
          </div>
        </div>

        <p className="mt-8 text-center text-sm text-muted-foreground">
          Don't have an account? <a href="#" className="font-bold text-primary hover:underline">Request API Access</a>
        </p>
      </div>
    </div>
  );
};
