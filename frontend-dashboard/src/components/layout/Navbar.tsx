import React from 'react';
import { Search, Bell, Menu, Circle } from 'lucide-react';

export const Navbar = () => {
  return (
    <header className="fixed right-0 top-0 z-30 h-16 w-full bg-white/70 backdrop-blur-md border-b border-border pl-64 transition-all">
      <div className="flex h-full items-center justify-between px-8">
        <div className="flex w-96 items-center gap-3 rounded-xl bg-muted px-4 py-2 ring-1 ring-border focus-within:ring-primary/20 transition-all">
          <Search className="h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search analytics, policies, or logs..."
            className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
          />
        </div>

        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2 rounded-full bg-success/10 px-3 py-1 text-xs font-medium text-success ring-1 ring-success/20">
            <Circle className="h-2 w-2 fill-current" />
            System Healthy
          </div>

          <div className="flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary ring-1 ring-primary/20">
            Demo Mode
          </div>

          <div className="flex items-center gap-4 border-l border-border pl-6">
            <button className="relative rounded-full p-2 text-muted-foreground hover:bg-muted hover:text-card-foreground transition-colors">
              <Bell className="h-5 w-5" />
              <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-destructive border-2 border-white" />
            </button>
            <div className="h-8 w-8 overflow-hidden rounded-full ring-2 ring-border">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Admin" alt="Profile" className="h-full w-full object-cover" />
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
