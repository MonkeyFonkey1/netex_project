import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { useAuth } from './authContext'
import { signUp } from './authApi'
import './auth.css'

export function SignupPage() {
  const { user, status } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [created, setCreated] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await signUp(email, password)
      setCreated(true)
      setPassword('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not create your account.')
    } finally {
      setBusy(false)
    }
  }

  if (user) return <Navigate to="/" replace />

  return (
    <main className="auth-page">
      <div className="auth-card">
        <p className="eyebrow">Account access</p>
        <h1>Create an account</h1>
        {created ? (
          <div className="auth-success" role="status">
            <p>Your account is ready. Sign in with {email.trim()} to continue.</p>
            <Link className="auth-submit auth-link-button" to="/login">Go to sign in</Link>
          </div>
        ) : (
          <>
            <p className="auth-intro">Create an account to add and manage contacts.</p>
            {status === 'loading' ? (
              <p role="status">Checking your session…</p>
            ) : (
              <form onSubmit={handleSubmit}>
                <label htmlFor="signup-email">Email</label>
                <input id="signup-email" type="email" autoComplete="email" required maxLength={254}
                  value={email} onChange={(event) => setEmail(event.target.value)} />

                <label htmlFor="signup-password">Password</label>
                <input id="signup-password" type="password" autoComplete="new-password"
                  required minLength={8} maxLength={72}
                  value={password} onChange={(event) => setPassword(event.target.value)} />
                <p className="auth-hint">Use 8–72 characters.</p>

                {error && <p className="auth-error" role="alert">{error}</p>}
                <button className="auth-submit" type="submit" disabled={busy}>
                  {busy ? 'Creating account…' : 'Create account'}
                </button>
              </form>
            )}
            <p className="auth-switch">Already registered? <Link to="/login">Sign in</Link></p>
          </>
        )}
      </div>
    </main>
  )
}
