import { createContext, useCallback, useContext, useState } from 'react'
import { login as loginRequest } from '../api/auth'
import { getStoredToken, getStoredUsername, setStoredAuth, clearStoredAuth } from '../auth/tokenStorage'

interface AuthCtx {
  token: string | null
  username: string | null
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  setSession: (token: string, username: string) => void
}

const Ctx = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken())
  const [username, setUsername] = useState<string | null>(() => getStoredUsername())

  const login = useCallback(async (u: string, p: string) => {
    const res = await loginRequest(u, p)
    setStoredAuth(res.token, res.username)
    setToken(res.token)
    setUsername(res.username)
  }, [])

  const logout = useCallback(() => {
    clearStoredAuth()
    setToken(null)
    setUsername(null)
  }, [])

  const setSession = useCallback((t: string, u: string) => {
    setStoredAuth(t, u)
    setToken(t)
    setUsername(u)
  }, [])

  return (
    <Ctx.Provider value={{ token, username, isAuthenticated: !!token, login, logout, setSession }}>
      {children}
    </Ctx.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useAuth must be inside AuthProvider')
  return ctx
}
