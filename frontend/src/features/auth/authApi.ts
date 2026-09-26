export type AuthUser = {
  id: number
  email: string
  role: 'USER' | 'ADMIN'
}

async function send(path: string, options?: RequestInit): Promise<Response> {
  try {
    return await (options?.method ? csrfFetch(path, options) : fetch(path, { credentials: 'same-origin' }))
  } catch {
    throw new Error('Cannot reach the server. Please try again.')
  }
}

export async function getCurrentUser(): Promise<AuthUser | null> {
  const response = await send('/api/auth/me')
  if (response.status === 401) return null
  if (!response.ok) throw new Error(`Could not check your session: HTTP ${response.status}`)
  return await response.json() as AuthUser
}

export async function signUp(email: string, password: string): Promise<AuthUser> {
  const response = await send('/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })

  if (response.status === 409) throw new Error('An account with this email already exists.')
  if (response.status === 400) throw new Error('Check your email and password, then try again.')
  if (!response.ok) throw new Error(`Could not create your account: HTTP ${response.status}`)
  return await response.json() as AuthUser
}

export async function signIn(email: string, password: string): Promise<void> {
  const response = await send('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ email, password }),
  })

  if (response.status === 401) throw new Error('Incorrect email or password.')
  if (!response.ok) throw new Error(`Could not sign in: HTTP ${response.status}`)
  clearCsrfToken()
}

export async function signOut(): Promise<void> {
  const response = await send('/api/auth/logout', { method: 'POST' })
  if (!response.ok) throw new Error(`Could not sign out: HTTP ${response.status}`)
  clearCsrfToken()
}
import { clearCsrfToken, csrfFetch } from './csrfFetch'
