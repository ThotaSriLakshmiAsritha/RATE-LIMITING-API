import { useState } from 'react'
import { Navbar } from './Navbar'
import { Sidebar } from './Sidebar'

export function LayoutWrapper({ health, children }) {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <div className="min-h-screen bg-app-gradient">
      <Navbar
        health={health}
        collapsed={collapsed}
        onToggleSidebar={() => setCollapsed((current) => !current)}
      />

      <div className="mx-auto grid max-w-[1400px] grid-cols-1 gap-4 px-3 pb-8 pt-4 lg:grid-cols-[260px_1fr] lg:px-6">
        <Sidebar collapsed={collapsed} />
        <main className="space-y-4">{children}</main>
      </div>
    </div>
  )
}
