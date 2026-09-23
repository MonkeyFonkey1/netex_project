import { useEffect, useState } from 'react'
import { getApiHealth } from './api/health'
import './App.css'

type ConnectionState = 'checking' | 'connected' | 'unavailable'

const connectionLabels: Record<ConnectionState, string> = {
  checking: 'Checking connection…',
  connected: 'Server connected',
  unavailable: 'Server unavailable',
}

export default function App() {
  const [connection, setConnection] = useState<ConnectionState>('checking')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    getApiHealth(controller.signal)
      .then((health) => {
        if (!controller.signal.aborted) {
          setConnection(health.status === 'UP' ? 'connected' : 'unavailable')
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setConnection('unavailable')
      })

    return () => controller.abort()
  }, [attempt])

  return (
    <main className="page">
      <header className="page-header">
        <span className="brand-mark" aria-hidden="true">N</span>
        <span>Netex · Address book</span>
      </header>
      <section className="welcome" aria-labelledby="welcome-title">
        <p className="eyebrow">Getting started</p>
        <h1 id="welcome-title">Your contacts,<br />in one place.</h1>
        <p className="intro">
          The address book is taking shape. This first step connects the
          interface to the server.
        </p>
        <div className="connection-panel">
          <div role="status" aria-live="polite">
            <span className={`status-dot ${connection}`} aria-hidden="true" />
            <strong>{connectionLabels[connection]}</strong>
          </div>
          <button
            type="button"
            disabled={connection === 'checking'}
            onClick={() => {
              setConnection('checking')
              setAttempt((value) => value + 1)
            }}
          >
            Check again
          </button>
        </div>
        {connection === 'unavailable' && (
          <p className="error-message">
            We could not reach the server. Start the backend and try again.
          </p>
        )}
      </section>
      <footer>Netex interview project · Step 1</footer>
    </main>
  )
}
