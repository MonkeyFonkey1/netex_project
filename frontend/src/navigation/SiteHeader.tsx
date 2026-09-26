import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import { useAuth } from '../features/auth/authContext'

export function SiteHeader() {
  const { user, status, logout } = useAuth()
  const navigate = useNavigate()
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLogoutError(null)
    setLoggingOut(true)
    try {
      await logout()
      navigate('/', { replace: true })
    } catch {
      setLogoutError('Could not sign out. Please try again.')
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <>
      <header className="site-header">
        <Link className="brand" to="/" aria-label="Netex address book, contacts">
          <span className="brand-mark" aria-hidden="true">N</span>
          <span>Netex <span className="brand-divider">/</span> Address book</span>
        </Link>

        <nav className="site-nav" aria-label="Main navigation">
          <NavLink end to="/" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            Contacts
          </NavLink>
          {status === 'loading' ? (
            <span className="session-status" role="status">Checking session…</span>
          ) : user ? (
            <>
              {user.role === 'ADMIN' && <NavLink to="/admin/activity"
                className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                Activity
              </NavLink>}
              <span className="signed-in-email" title={user.email}>{user.email}</span>
              <button className="nav-link nav-button" type="button" onClick={handleLogout}
                disabled={loggingOut}>
                {loggingOut ? 'Signing out…' : 'Sign out'}
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                Sign in
              </NavLink>
              <NavLink to="/signup" className={({ isActive }) => isActive ? 'nav-link nav-link-primary active' : 'nav-link nav-link-primary'}>
                Create account
              </NavLink>
            </>
          )}
        </nav>
      </header>
      {logoutError && <p className="header-error" role="alert">{logoutError}</p>}
    </>
  )
}
