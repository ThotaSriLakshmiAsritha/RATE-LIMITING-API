import React, { useState } from 'react';
import { 
  Plus, 
  Search, 
  MoreVertical, 
  Edit2, 
  Trash2, 
  Globe, 
  User, 
  Key, 
  Monitor,
  X
} from 'lucide-react';
import { cn } from '../lib/utils';

const initialPolicies = [
  { id: 1, name: 'Global Default', limit: '100 req/min', burst: '20', scope: 'Global', dimension: 'IP', status: 'Active' },
  { id: 2, name: 'Premium User', limit: '1000 req/min', burst: '100', scope: 'User', dimension: 'USER_ID', status: 'Active' },
  { id: 3, name: 'Free Tier', limit: '10 req/min', burst: '2', scope: 'Global', dimension: 'IP', status: 'Active' },
  { id: 4, name: 'Internal Services', limit: 'Unlimited', burst: 'N/A', scope: 'Global', dimension: 'IP', status: 'Bypassed' },
];

export const Policies = () => {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedPolicy, setSelectedPolicy] = useState<any>(null);

  const openEdit = (policy: any) => {
    setSelectedPolicy(policy);
    setIsDrawerOpen(true);
  };

  return (
    <div className="space-y-8 animate-slide-up relative overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-card-foreground">Policies</h1>
          <p className="text-muted-foreground mt-1">Manage rate limits and bypass rules for your endpoints.</p>
        </div>
        <button className="btn btn-primary gap-2 shadow-lg shadow-primary/20">
          <Plus className="h-4 w-4" />
          Create Policy
        </button>
      </div>

      <div className="card overflow-hidden">
        <div className="p-6 border-b border-border flex items-center justify-between bg-white/50">
          <div className="flex w-80 items-center gap-3 rounded-lg bg-muted px-3 py-1.5 ring-1 ring-border focus-within:ring-primary/20 transition-all">
            <Search className="h-4 w-4 text-muted-foreground" />
            <input type="text" placeholder="Search policies..." className="bg-transparent text-sm outline-none w-full" />
          </div>
          <div className="flex gap-2">
            <button className="btn btn-ghost text-xs">All Scopes</button>
            <button className="btn btn-ghost text-xs">Active Only</button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="bg-muted/50 text-muted-foreground font-medium border-b border-border">
                <th className="px-6 py-4">Policy Name</th>
                <th className="px-6 py-4">Limit</th>
                <th className="px-6 py-4">Burst</th>
                <th className="px-6 py-4">Scope</th>
                <th className="px-6 py-4">Dimension</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border bg-white">
              {initialPolicies.map((policy) => (
                <tr key={policy.id} className="hover:bg-muted/30 transition-colors group">
                  <td className="px-6 py-4 font-semibold text-card-foreground">{policy.name}</td>
                  <td className="px-6 py-4">{policy.limit}</td>
                  <td className="px-6 py-4">{policy.burst}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      {policy.scope === 'Global' ? <Globe className="h-3 w-3 text-primary" /> : <User className="h-3 w-3 text-secondary" />}
                      {policy.scope}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="bg-muted px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wider text-muted-foreground">
                      {policy.dimension}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`badge ${policy.status === 'Active' ? 'bg-success/10 text-success' : 'bg-muted text-muted-foreground'}`}>
                      {policy.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button onClick={() => openEdit(policy)} className="p-1 rounded-md hover:bg-muted text-muted-foreground hover:text-primary transition-all opacity-0 group-hover:opacity-100">
                      <Edit2 className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Drawer Overlay */}
      {isDrawerOpen && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20 backdrop-blur-sm" onClick={() => setIsDrawerOpen(false)} />
          <div className="relative w-[450px] h-full bg-white shadow-2xl animate-fade-in border-l border-border flex flex-col">
            <div className="p-6 border-b border-border flex items-center justify-between">
              <h2 className="text-xl font-bold">Edit Policy</h2>
              <button onClick={() => setIsDrawerOpen(false)} className="p-2 rounded-full hover:bg-muted transition-colors">
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="flex-1 p-8 space-y-8 overflow-y-auto">
              <div className="space-y-2">
                <label className="text-sm font-semibold">Policy Name</label>
                <input 
                  type="text" 
                  defaultValue={selectedPolicy?.name} 
                  className="w-full rounded-lg border border-border p-3 outline-none focus:ring-2 focus:ring-primary/20 transition-all" 
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-semibold">Requests per Window</label>
                  <input type="number" defaultValue={100} className="w-full rounded-lg border border-border p-3 outline-none focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-semibold">Window (seconds)</label>
                  <input type="number" defaultValue={60} className="w-full rounded-lg border border-border p-3 outline-none focus:ring-2 focus:ring-primary/20 transition-all" />
                </div>
              </div>

              <div className="space-y-4">
                <label className="text-sm font-semibold">Configuration Scope</label>
                <div className="grid grid-cols-3 gap-3">
                  {[
                    { id: 'global', icon: Globe, label: 'Global' },
                    { id: 'user', icon: User, label: 'User' },
                    { id: 'apikey', icon: Key, label: 'API Key' },
                  ].map((scope) => (
                    <button 
                      key={scope.id}
                      className={`flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all ${
                        selectedPolicy?.scope.toLowerCase() === scope.id ? 'border-primary bg-primary/5 text-primary' : 'border-border text-muted-foreground hover:border-primary/50'
                      }`}
                    >
                      <scope.icon className="h-5 w-5" />
                      <span className="text-xs font-bold uppercase">{scope.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-semibold">Error Message (Optional)</label>
                <textarea 
                  placeholder="Too many requests. Please try again later."
                  className="w-full h-32 rounded-lg border border-border p-3 outline-none focus:ring-2 focus:ring-primary/20 transition-all resize-none"
                />
              </div>
            </div>
            <div className="p-6 border-t border-border bg-muted/30 flex gap-3">
              <button className="btn btn-primary flex-1 py-3 text-base shadow-lg shadow-primary/20">Save Changes</button>
              <button className="btn btn-ghost border border-border bg-white flex-1 py-3 text-base" onClick={() => setIsDrawerOpen(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
