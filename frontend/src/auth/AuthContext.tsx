import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { post, setUnauthorizedHandler, TOKEN_KEY } from '../lib/api'
import type { AuthResponse, Role } from '../lib/types'

// Mirrors the backend role hierarchy: ADMIN > MANAGER > STAFF.
const RANK: Record<Role, number> = { STAFF: 1, MANAGER: 2, ADMIN: 3 }

interface AuthState {
  user: AuthResponse | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  // UI convenience only: hides buttons the user can't use.
  // The real check is @PreAuthorize on the server.
  hasRole: (role: Role) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

function loadUser(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(TOKEN_KEY)
    return raw ? (JSON.parse(raw) as AuthResponse) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(loadUser)

  const logout = useCallback(() => {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* ignore */
    }
    setUser(null)
  }, [])

  useEffect(() => setUnauthorizedHandler(logout), [logout])

  const login = async (email: string, password: string) => {
    const res = await post<AuthResponse>('/api/auth/login', { email, password })
    localStorage.setItem(TOKEN_KEY, JSON.stringify(res))
    setUser(res)
  }

  const hasRole = (role: Role) => !!user && RANK[user.role] >= RANK[role]

  return <AuthContext.Provider value={{ user, login, logout, hasRole }}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
