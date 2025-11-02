import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

const AuthContext = createContext({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  login: async () => {},
  logout: async () => {},
  getCsrfToken: () => null,
  updateUser: () => {}
})

const STORAGE_KEYS = {
  persistentUser: 'user',
  rememberFlag: 'rememberUser',
  sessionUser: 'sessionUser'
}

const isBrowser = typeof window !== 'undefined'

const isRememberPreferenceEnabled = () => {
  if (!isBrowser) {
    return false
  }
  return window.localStorage.getItem(STORAGE_KEYS.rememberFlag) === 'true'
}

const readStoredUser = () => {
  if (!isBrowser) {
    return null
  }
  try {
    const sessionValue = window.sessionStorage.getItem(STORAGE_KEYS.sessionUser)
    if (sessionValue) {
      return JSON.parse(sessionValue)
    }

    if (isRememberPreferenceEnabled()) {
      const raw = window.localStorage.getItem(STORAGE_KEYS.persistentUser)
      return raw ? JSON.parse(raw) : null
    }

    return null
  } catch (error) {
    console.error('Failed to parse stored user', error)
    window.localStorage.removeItem(STORAGE_KEYS.persistentUser)
    window.localStorage.removeItem(STORAGE_KEYS.rememberFlag)
    window.sessionStorage.removeItem(STORAGE_KEYS.sessionUser)
    return null
  }
}

const shouldRestoreSession = () => {
  if (!isBrowser) {
    return false
  }

  if (window.sessionStorage.getItem(STORAGE_KEYS.sessionUser)) {
    return true
  }

  return isRememberPreferenceEnabled()
}

const extractCsrfToken = () => {
  if (typeof document === 'undefined') {
    return null
  }
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => readStoredUser())
  const [isLoading, setIsLoading] = useState(true)
  const persistUser = useCallback((nextUser, options = {}) => {
    const { remember } = options
    setUser(nextUser)

    if (!isBrowser) {
      return
    }

    if (!nextUser) {
      window.sessionStorage.removeItem(STORAGE_KEYS.sessionUser)
      window.localStorage.removeItem(STORAGE_KEYS.persistentUser)
      window.localStorage.removeItem(STORAGE_KEYS.rememberFlag)
      return
    }

    const serialized = JSON.stringify(nextUser)
    const effectiveRemember = remember ?? isRememberPreferenceEnabled()

    window.sessionStorage.setItem(STORAGE_KEYS.sessionUser, serialized)

    if (effectiveRemember) {
      window.localStorage.setItem(STORAGE_KEYS.persistentUser, serialized)
      window.localStorage.setItem(STORAGE_KEYS.rememberFlag, 'true')
    } else {
      window.localStorage.removeItem(STORAGE_KEYS.persistentUser)
      window.localStorage.removeItem(STORAGE_KEYS.rememberFlag)
    }
  }, [])

  useEffect(() => {
    let isMounted = true

    const restoreSession = async () => {
      if (!shouldRestoreSession()) {
        setIsLoading(false)
        return
      }

      try {
        const response = await fetch('/api/auth/session', {
          credentials: 'include'
        })

        if (!isMounted) {
          return
        }

        if (response.ok) {
          const data = await response.json()
          const remember = isRememberPreferenceEnabled()
          persistUser(data, { remember })
        } else {
          persistUser(null)
        }
      } catch (error) {
        console.error('Failed to restore session', error)
        if (isMounted) {
          persistUser(null)
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    restoreSession()

    return () => {
      isMounted = false
    }
  }, [persistUser])

  const login = useCallback(async (username, password, rememberMe = false) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, password, rememberMe })
    })

    if (!response.ok) {
      throw new Error('Invalid username or password')
    }

    const data = await response.json()
    persistUser(data, { remember: rememberMe })
    return data
  }, [persistUser])

  const logout = useCallback(async () => {
    try {
      const csrfToken = extractCsrfToken()
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
        headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}
      })
    } catch (error) {
      console.error('Logout request failed', error)
    } finally {
      persistUser(null)
    }
  }, [persistUser])

  const value = useMemo(() => ({
    user,
    isAuthenticated: Boolean(user),
    isLoading,
    login,
    logout,
    getCsrfToken: extractCsrfToken,
    updateUser: persistUser
  }), [user, isLoading, login, logout, persistUser])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
