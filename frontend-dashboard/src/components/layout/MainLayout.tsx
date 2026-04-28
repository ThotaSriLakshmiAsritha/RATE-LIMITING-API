import React from 'react';
import { Sidebar } from './Sidebar';
import { Navbar } from './Navbar';

interface MainLayoutProps {
  children: React.ReactNode;
}

export const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <Navbar />
      <main className="pl-64 pt-16 transition-all duration-300">
        <div className="mx-auto max-w-[1600px] p-8 animate-fade-in">
          {children}
        </div>
      </main>
    </div>
  );
};
