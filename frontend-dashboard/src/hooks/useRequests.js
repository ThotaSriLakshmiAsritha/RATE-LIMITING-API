import { useMemo, useState } from 'react'
import { createProduct, getProducts, login, ping } from '../services/apiClient'

const formatLabel = (method, path) => `${method} ${path}`

export function useRequests() {
  const [token, setToken] = useState('')
  const [history, setHistory] = useState([])
  const [inFlight, setInFlight] = useState(false)
  const [latestResponse, setLatestResponse] = useState(null)
  const [notification, setNotification] = useState(null)

  const recordResult = (path, result) => {
    const entry = {
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      endpoint: path,
      method: result.method,
      status: result.status,
      durationMs: result.durationMs,
      label: formatLabel(result.method, path),
      rateLimit: result.rateLimit,
      payload: result.data,
    }

    setHistory((previous) => [entry, ...previous].slice(0, 40))
    setLatestResponse(entry)

    if (result.status === 429) {
      const retryAfter = result.rateLimit.retryAfter || 'a few seconds'
      setNotification({
        type: 'warning',
        message: `Rate limit reached. Retry after ${retryAfter}.`,
      })
      return
    }

    if (!result.ok) {
      setNotification({
        type: 'error',
        message: `Request failed (${result.status}) on ${path}.`,
      })
    }
  }

  const runRequest = async (path, callback) => {
    setInFlight(true)
    try {
      const result = await callback()
      recordResult(path, result)
      return result
    } finally {
      setInFlight(false)
    }
  }

  const actions = {
    runPing: () => runRequest('/ping', () => ping()),
    runLogin: async (credentials) => {
      const result = await runRequest('/api/auth/login', () => login(credentials))

      const issuedToken =
        result?.data?.access_token ||
        result?.data?.accessToken ||
        result?.data?.token
      if (issuedToken) {
        setToken(issuedToken)
        setNotification({
          type: 'success',
          message: 'Login succeeded and token was stored for subsequent requests.',
        })
      }

      return result
    },
    runGetProducts: () => {
      if (!token) {
        setNotification({
          type: 'warning',
          message: 'Login first to call protected product endpoints.',
        })
        return Promise.resolve(null)
      }

      return runRequest('/api/v1/products', () => getProducts(token))
    },
    runCreateProduct: (product) => {
      if (!token) {
        setNotification({
          type: 'warning',
          message: 'Login first to create products.',
        })
        return Promise.resolve(null)
      }

      return runRequest('/api/v1/products', () => createProduct(token, product))
    },
    clearNotification: () => setNotification(null),
    clearHistory: () => setHistory([]),
    setToken,
  }

  const latestRateLimit = useMemo(() => latestResponse?.rateLimit || null, [latestResponse])

  return {
    token,
    history,
    inFlight,
    latestResponse,
    latestRateLimit,
    notification,
    ...actions,
  }
}
