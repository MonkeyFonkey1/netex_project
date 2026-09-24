import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <main className="placeholder-page">
      <p className="eyebrow">Page not found</p>
      <h1>This page doesn’t exist.</h1>
      <p>Check the address or return to the contacts directory.</p>
      <Link className="page-link" to="/">View contacts</Link>
    </main>
  )
}
