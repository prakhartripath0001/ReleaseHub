const routes = {
  catalog: '/api/catalog',
  search: '/api/search',
  feedback: '/api/feedback',
  reports: '/api/reports',
}

export async function apiRequest(service, path, options = {}, settings = {}) {
  const origin = settings.origin?.trim().replace(/\/+$/, '') ?? ''
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (settings.token?.trim()) {
    headers.set('Authorization', `Bearer ${settings.token.trim()}`)
  }
  if (settings.role) {
    headers.set('X-Role', settings.role)
    headers.set('X-User-Role', settings.role)
  }
  if (settings.userId?.trim()) {
    headers.set('X-User-Id', settings.userId.trim())
  }

  const response = await fetch(`${origin}${routes[service]}${path}`, {
    ...options,
    headers,
  })
  const contentType = response.headers.get('content-type') ?? ''
  const result = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = typeof result === 'string'
      ? result
      : result.message ?? result.detail ?? result.error ?? JSON.stringify(result)
    throw new Error(`${response.status} ${response.statusText}: ${message || 'Request failed'}`)
  }
  return result
}

export function queryString(values) {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value !== undefined && value !== null) {
      params.set(key, value)
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}
