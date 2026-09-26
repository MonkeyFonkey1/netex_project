import { useEffect, useState, type ReactNode } from 'react'
import { getCurrentUser, signIn, signOut, type AuthUser } from './authApi'
import { AuthContext, type AuthStatus } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')

  useEffect(() => {
    let active = true
    getCurrentUser()
      .then((account) => {
        if (!active) return
        setUser(account)
        setStatus('ready')
      })
      .catch(() => {
        if (active) setStatus('error')
      })

    return () => { active = false }
  }, [])

  async function login(email: string, password: string) {
    await signIn(email, password)
    const account = await getCurrentUser()
    if (!account) throw new Error('Sign in succeeded, but your session could not be found.')
    setUser(account)
    setStatus('ready')
  }

  async function logout() {
    await signOut()
    setUser(null)
    setStatus('ready')
  }

  return (
    <AuthContext.Provider value={{ user, status, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
