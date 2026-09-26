import { useEffect, useRef, useState, type ReactNode } from 'react'
import { getCurrentUser, signIn, signOut, type AuthUser } from './authApi'
import { AuthContext, type AuthStatus } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')
  const sessionCheck = useRef(0)

  useEffect(() => {
    let active = true
    function refreshSession(initial = false) {
      const check = ++sessionCheck.current
      getCurrentUser()
        .then((account) => {
          if (!active || check !== sessionCheck.current) return
          setUser(account)
          setStatus('ready')
        })
        .catch(() => {
          if (active && initial && check === sessionCheck.current) setStatus('error')
        })
    }

    function onVisible() {
      if (document.visibilityState === 'visible') refreshSession()
    }

    refreshSession(true)
    window.addEventListener('focus', onVisible)
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      active = false
      window.removeEventListener('focus', onVisible)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [])

  useEffect(() => {
    function sessionExpired() {
      sessionCheck.current++
      setUser(null)
      setStatus('ready')
    }
    window.addEventListener('netex-session-expired', sessionExpired)
    return () => window.removeEventListener('netex-session-expired', sessionExpired)
  }, [])

  async function login(email: string, password: string) {
    await signIn(email, password)
    const account = await getCurrentUser()
    if (!account) throw new Error('Sign in succeeded, but your session could not be found.')
    sessionCheck.current++
    setUser(account)
    setStatus('ready')
  }

  async function logout() {
    await signOut()
    sessionCheck.current++
    setUser(null)
    setStatus('ready')
  }

  return (
    <AuthContext.Provider value={{ user, status, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
