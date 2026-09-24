import { Link } from 'react-router'

export function AdminActivityPlaceholderPage() {
  return (
    <main className="placeholder-page">
      <p className="eyebrow">Administration</p>
      <h1>Activity history</h1>
      <p>This page will show processed signup and contact activity after admin access is implemented.</p>
      <Link className="page-link" to="/">View public contacts</Link>
    </main>
  )
}
