import { Navigate, Route, Routes } from 'react-router-dom'
import { LayoutWrapper } from './components/layout/LayoutWrapper'
import { useMetrics } from './hooks/useMetrics'
import { useRequests } from './hooks/useRequests'
import { Analytics } from './pages/Analytics'
import { Dashboard } from './pages/Dashboard'
import { RateLimits } from './pages/RateLimits'
import { Settings } from './pages/Settings'
import { getApiBaseUrl } from './services/apiClient'

function Toast({ notification, onClose }) {
  if (!notification) {
    return null
  }

  return (
    <aside className="toast-shell" role="status" aria-live="polite">
      <p className="text-sm font-medium text-white">{notification.message}</p>
      <button type="button" onClick={onClose} className="ml-3 text-xs uppercase tracking-[0.18em] text-white/80">
        Dismiss
      </button>
    </aside>
  )
}

function App() {
  const requestApi = useRequests()
  const metrics = useMetrics(requestApi.history)

  return (
    <>
      <LayoutWrapper health={metrics.health}>
        <Routes>
          <Route path="/" element={<Dashboard metrics={metrics} requestApi={requestApi} />} />
          <Route path="/analytics" element={<Analytics metrics={metrics} />} />
          <Route
            path="/rate-limits"
            element={
              <RateLimits
                latestRateLimit={requestApi.latestRateLimit}
                history={requestApi.history}
                onClearHistory={requestApi.clearHistory}
              />
            }
          />
          <Route path="/settings" element={<Settings baseUrl={getApiBaseUrl()} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </LayoutWrapper>

      <Toast notification={requestApi.notification} onClose={requestApi.clearNotification} />
    </>
  )
}

export default App
