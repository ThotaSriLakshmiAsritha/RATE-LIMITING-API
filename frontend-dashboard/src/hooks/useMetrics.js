import { useEffect, useMemo, useState } from 'react'
import { ping } from '../services/apiClient'

export function useMetrics(history) {
  const [health, setHealth] = useState({
    loading: true,
    ok: false,
    status: null,
    checkedAt: null,
  })

  useEffect(() => {
    let active = true

    const checkHealth = async () => {
      try {
        const result = await ping()
        if (!active) {
          return
        }

        setHealth({
          loading: false,
          ok: result.ok,
          status: result.status,
          checkedAt: new Date().toISOString(),
        })
      } catch {
        if (!active) {
          return
        }

        setHealth({
          loading: false,
          ok: false,
          status: 'offline',
          checkedAt: new Date().toISOString(),
        })
      }
    }

    checkHealth()
    const intervalId = setInterval(checkHealth, 15000)

    return () => {
      active = false
      clearInterval(intervalId)
    }
  }, [])

  return useMemo(() => {
    const total = history.length
    const success = history.filter((entry) => entry.status >= 200 && entry.status < 300).length
    const throttled = history.filter((entry) => entry.status === 429).length
    const errors = history.filter((entry) => entry.status >= 400 && entry.status !== 429).length

    const lineSeries = history
      .slice(0, 12)
      .reverse()
      .map((entry, index) => ({
        x: index,
        y: Math.max(0, Math.min(100, 100 - entry.durationMs / 4)),
        status: entry.status,
      }))

    return {
      health,
      totals: {
        total,
        success,
        throttled,
        errors,
      },
      lineSeries,
    }
  }, [health, history])
}
