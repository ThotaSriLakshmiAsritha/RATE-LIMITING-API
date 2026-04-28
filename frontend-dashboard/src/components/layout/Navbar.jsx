import { Button } from '../ui/Button'
import { Badge } from '../ui/Badge'

export function Navbar({ health, onToggleSidebar, collapsed }) {
  const healthTone = health.ok ? 'success' : health.loading ? 'neutral' : 'error'
  const healthText = health.loading ? 'Checking backend' : health.ok ? 'Backend online' : 'Backend unreachable'

  return (
    <header className="sticky top-0 z-30 border-b border-ink-200/70 bg-fog-50/90 px-4 py-3 backdrop-blur lg:px-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-ink-500">Rate Limiting Control Plane</p>
          <h1 className="text-xl font-bold text-ink-900">Operational Dashboard</h1>
        </div>
        <div className="flex items-center gap-3">
          <Badge tone={healthTone}>{healthText}</Badge>
          <Button variant="ghost" onClick={onToggleSidebar}>
            {collapsed ? 'Open Menu' : 'Collapse'}
          </Button>
        </div>
      </div>
    </header>
  )
}
