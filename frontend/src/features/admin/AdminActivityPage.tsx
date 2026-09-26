import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router'
import { useAuth } from '../auth/authContext'
import { getActivityHistory, type ActivityHistory } from './activityApi'
import './admin.css'

function dateTime(value: string) {
  return new Date(value).toLocaleString()
}

function ActivityContent() {
  const [history, setHistory] = useState<ActivityHistory | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refresh, setRefresh] = useState(0)

  function reload() {
    setLoading(true)
    setError(null)
    setRefresh((value) => value + 1)
  }

  useEffect(() => {
    const controller = new AbortController()
    getActivityHistory(controller.signal)
      .then((result) => {
        setHistory(result)
        setLoading(false)
      })
      .catch((cause) => {
        if (controller.signal.aborted) return
        setError(cause instanceof Error ? cause.message : 'Could not load activity.')
        setLoading(false)
      })
    return () => controller.abort()
  }, [refresh])

  return (
    <main className="activity-page">
      <div className="activity-heading">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Activity history</h1>
          <p>Recent signup messages from Kafka and contact changes received over HTTP.</p>
        </div>
        <button className="activity-refresh" type="button" onClick={reload}
          disabled={loading}>Refresh</button>
      </div>

      {loading && <p role="status">Loading activity…</p>}
      {error && <div className="activity-error" role="alert">
        <p>{error}</p>
        <button type="button" onClick={reload}>Try again</button>
      </div>}

      {history && !loading && !error && <>
        <section className="activity-section" aria-labelledby="signup-history-title">
          <div className="activity-section-heading">
            <div>
              <p className="section-label">Kafka consumer</p>
              <h2 id="signup-history-title">User signups</h2>
            </div>
            <span>{history.signups.length} recent events</span>
          </div>
          {history.signups.length === 0 ? <p className="activity-empty">No signups have been processed yet.</p> :
            <div className="activity-table-scroll"><table>
              <thead><tr><th>User ID</th><th>Email</th><th>Signed up</th><th>Processed</th></tr></thead>
              <tbody>{history.signups.map((item) => <tr key={item.userId}>
                <td>{item.userId}</td><td>{item.email}</td>
                <td>{dateTime(item.signedUpAt)}</td><td>{dateTime(item.processedAt)}</td>
              </tr>)}</tbody>
            </table></div>}
        </section>

        <section className="activity-section" aria-labelledby="contact-history-title">
          <div className="activity-section-heading">
            <div>
              <p className="section-label">HTTP microservice</p>
              <h2 id="contact-history-title">Contact changes</h2>
            </div>
            <span>{history.contactChanges.length} recent events</span>
          </div>
          {history.contactChanges.length === 0 ? <p className="activity-empty">No contact changes have been recorded yet.</p> :
            <div className="activity-table-scroll"><table>
              <thead><tr><th>Action</th><th>Contact ID</th><th>User ID</th><th>Occurred</th><th>Processed</th></tr></thead>
              <tbody>{history.contactChanges.map((item) => <tr key={item.eventId}>
                <td><span className="activity-action">{item.action}</span></td>
                <td>{item.contactId}</td><td>{item.actorUserId}</td>
                <td>{dateTime(item.occurredAt)}</td><td>{dateTime(item.processedAt)}</td>
              </tr>)}</tbody>
            </table></div>}
        </section>
      </>}
    </main>
  )
}

export function AdminActivityPage() {
  const { user, status } = useAuth()
  if (status === 'loading') return <main className="activity-page"><p role="status">Checking your session…</p></main>
  if (status === 'error') return <main className="activity-page"><p role="alert">Could not check your session. Reload the page to try again.</p></main>
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== 'ADMIN') return <main className="activity-page">
    <h1>Admin access required</h1>
    <p>This page is available only to an admin account.</p>
    <Link to="/">Back to contacts</Link>
  </main>
  return <ActivityContent />
}
