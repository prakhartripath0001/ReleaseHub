import { useEffect, useState } from 'react'

const initialSettings = {
  origin: localStorage.getItem('releasehub.apiOrigin') ?? '',
  token: sessionStorage.getItem('releasehub.authToken') ?? '',
  role: localStorage.getItem('releasehub.role') ?? 'PUBLISHER',
  userId: localStorage.getItem('releasehub.userId') ?? '',
}

export function useAuth() {
  const [settings, setSettings] = useState(initialSettings)

  useEffect(() => {
    localStorage.setItem('releasehub.apiOrigin', settings.origin)
    if (settings.token) sessionStorage.setItem('releasehub.authToken', settings.token)
    else sessionStorage.removeItem('releasehub.authToken')
    localStorage.setItem('releasehub.role', settings.role)
    localStorage.setItem('releasehub.userId', settings.userId)
  }, [settings])

  return { settings, setSettings }
}
