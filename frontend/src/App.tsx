import { useEffect, useState } from 'react'
import { getContacts, type Contact } from './api/contacts'
import { ContactCard } from './components/ContactCard'
import './App.css'

type LoadStatus = 'loading' | 'success' | 'error'

export default function App() {
  const [search, setSearch] = useState('')
  const [contacts, setContacts] = useState<Contact[]>([])
  const [status, setStatus] = useState<LoadStatus>('loading')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const name = search.trim()
    // Wait while the user types, then cancel older requests if the search changes.
    const timer = window.setTimeout(() => {
      getContacts(name, controller.signal)
        .then((result) => {
          if (controller.signal.aborted) return
          setContacts(result)
          setStatus('success')
        })
        .catch(() => {
          if (!controller.signal.aborted) setStatus('error')
        })
    }, name ? 250 : 0)

    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [search, retry])

  const isSearching = search.trim().length > 0
  const itemLabel = isSearching
    ? (contacts.length === 1 ? 'result' : 'results')
    : (contacts.length === 1 ? 'contact' : 'contacts')

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">N</span>
          <span>Netex <span className="brand-divider">/</span> Address book</span>
        </div>
        <span className="header-note">A shared directory</span>
      </header>

      <main>
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Your address book</p>
          <h1 id="page-title">People, all in one place.</h1>
          <p className="hero-description">
            Find the contact you need by name and see their address at a glance.
          </p>
        </section>

        <section className="directory" aria-labelledby="directory-title">
          <div className="directory-heading">
            <div>
              <p className="section-label">DIRECTORY</p>
              <h2 id="directory-title">Contacts</h2>
            </div>
            {status === 'success' && (
              <span className="result-count">
                {contacts.length} {itemLabel}
              </span>
            )}
          </div>

          <div className="search-field">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
              <circle cx="10.8" cy="10.8" r="6.8" />
              <path d="m16 16 5 5" />
            </svg>
            <label className="visually-hidden" htmlFor="contact-search">Search contacts by name</label>
            <input
              id="contact-search"
              type="search"
              placeholder="Search by name"
              autoComplete="off"
              maxLength={255}
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setStatus('loading')
              }}
            />
          </div>

          <div className="list-panel" aria-live="polite">
            {status === 'loading' && (
              <div className="message-state" role="status">
                <span className="loading-indicator" aria-hidden="true" />
                <p>{isSearching ? 'Searching contacts…' : 'Loading contacts…'}</p>
              </div>
            )}

            {status === 'error' && (
              <div className="message-state" role="alert">
                <span className="message-icon" aria-hidden="true">!</span>
                <h3>We couldn’t load the contacts</h3>
                <p>Check that the backend is running, then try again.</p>
                <button
                  type="button"
                  onClick={() => {
                    setStatus('loading')
                    setRetry((value) => value + 1)
                  }}
                >
                  Try again
                </button>
              </div>
            )}

            {status === 'success' && contacts.length === 0 && (
              <div className="message-state">
                <span className="empty-icon" aria-hidden="true">{isSearching ? '?' : 'N'}</span>
                <h3>{isSearching ? 'No matching contacts' : 'No contacts yet'}</h3>
                <p>
                  {isSearching
                    ? 'Try another name or clear your search.'
                    : 'When someone adds a contact, it will appear here.'}
                </p>
                {isSearching && (
                  <button
                    type="button"
                    className="text-button"
                    onClick={() => {
                      setSearch('')
                      setStatus('loading')
                    }}
                  >
                    Clear search
                  </button>
                )}
              </div>
            )}

            {status === 'success' && contacts.length > 0 && (
              <ul className="contact-list">
                {contacts.map((contact) => <ContactCard key={contact.id} contact={contact} />)}
              </ul>
            )}
          </div>
        </section>
      </main>

      <footer className="site-footer">Netex address book</footer>
    </div>
  )
}
