const DEFAULT_BASE_URL = ''

export const getApiBaseUrl = () => {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL
  return (configuredBaseUrl || DEFAULT_BASE_URL).replace(/\/$/, '')
}

const parseJsonSafely = async (response) => {
  try {
    return await response.json()
  } catch {
    return null
  }
}

const extractRateLimit = (headers) => ({
  limit: headers.get('x-ratelimit-limit'),
  remaining: headers.get('x-ratelimit-remaining'),
  reset: headers.get('x-ratelimit-reset'),
  retryAfter: headers.get('retry-after'),
})

export const apiRequest = async ({ path, method = 'GET', token, body }) => {
  const url = `${getApiBaseUrl()}${path}`
  const headers = {
    'Content-Type': 'application/json',
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const startedAt = performance.now()
  const response = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const durationMs = Math.round(performance.now() - startedAt)
  const data = await parseJsonSafely(response)

  return {
    ok: response.ok,
    status: response.status,
    data,
    durationMs,
    url,
    method,
    rateLimit: extractRateLimit(response.headers),
  }
}

export const ping = () => apiRequest({ path: '/ping' })

export const login = (credentials) =>
  apiRequest({
    path: '/api/auth/login',
    method: 'POST',
    body: credentials,
  })

export const getProducts = (token) =>
  apiRequest({
    path: '/api/v1/products',
    token,
  })

export const createProduct = (token, product) =>
  apiRequest({
    path: '/api/v1/products',
    method: 'POST',
    token,
    body: product,
  })
