import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { useAuth } from './authContext'
import './auth.css'

export function LoginPage() {
  const { user, status, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not sign in.')
    } finally {
      setBusy(false)
    }
  }

  if (user) return <Navigate to="/" replace />

  return (
    <main className="auth-page">
      <div className="auth-card">
        <p className="eyebrow">Account access</p>
        <h1>Sign in</h1>
        <p className="auth-intro">Welcome back. Sign in to manage your contacts.</p>

        {status === 'loading' ? (
          <p role="status">Checking your session…</p>
        ) : (
          <form onSubmit={handleSubmit}>
            <label htmlFor="login-email">Email</label>
            <input id="login-email" type="email" autoComplete="email" required
              value={email} onChange={(event) => setEmail(event.target.value)} />

            <label htmlFor="login-password">Password</label>
            <input id="login-password" type="password" autoComplete="current-password" required
              value={password} onChange={(event) => setPassword(event.target.value)} />

            {error && <p className="auth-error" role="alert">{error}</p>}
            <button className="auth-submit" type="submit" disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        )}

        <p className="auth-switch">No account yet? <Link to="/signup">Create one</Link></p>
      </div>
    </main>
  )
}
