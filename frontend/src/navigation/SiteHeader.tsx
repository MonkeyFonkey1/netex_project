import { Link, NavLink } from 'react-router'

export function SiteHeader() {
  return (
    <header className="site-header">
      <Link className="brand" to="/" aria-label="Netex address book, contacts">
        <span className="brand-mark" aria-hidden="true">N</span>
        <span>Netex <span className="brand-divider">/</span> Address book</span>
      </Link>

      <nav className="site-nav" aria-label="Main navigation">
        <NavLink end to="/" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
          Contacts
        </NavLink>
        <NavLink to="/login" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
          Sign in
        </NavLink>
        <NavLink to="/signup" className={({ isActive }) => isActive ? 'nav-link nav-link-primary active' : 'nav-link nav-link-primary'}>
          Create account
        </NavLink>
      </nav>
    </header>
  )
}
