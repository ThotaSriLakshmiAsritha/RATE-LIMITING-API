import { NavLink } from 'react-router-dom'

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/analytics', label: 'Analytics' },
  { to: '/rate-limits', label: 'Rate Limits' },
  { to: '/settings', label: 'Settings' },
]

export function Sidebar({ collapsed }) {
  return (
    <aside className={`sidebar-shell ${collapsed ? 'sidebar-collapsed' : ''}`}>
      <div className="mb-8">
        <p className="text-xs uppercase tracking-[0.25em] text-ink-500">Mongo Inspired</p>
        <p className="mt-2 text-base font-semibold text-ink-900">Atlas Pulse</p>
      </div>

      <nav className="space-y-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `nav-link ${isActive ? 'nav-link-active' : ''}`
            }
            end={item.to === '/'}
          >
            <span className="text-sm font-medium">{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
