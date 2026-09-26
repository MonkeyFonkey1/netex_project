import { useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/authContext'
import { contactsExportUrl, type Contact } from './contactsApi'
import { ContactForm } from './ContactForm'
import { ContactList } from './ContactList'
import { ContactSearch } from './ContactSearch'
import { useContacts } from './useContacts'
import './contacts.css'

export function ContactsPage() {
  const { user, status: authStatus } = useAuth()
  const { search, contacts, status, changeSearch, clearSearch, retryLoad } = useContacts()
  const [editor, setEditor] = useState<{ mode: 'create' } | { mode: 'edit'; contact: Contact } | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const isSearching = search.trim().length > 0
  const itemLabel = isSearching
    ? (contacts.length === 1 ? 'result' : 'results')
    : (contacts.length === 1 ? 'contact' : 'contacts')

  function refreshAfterChange() {
    if (isSearching) clearSearch()
    else retryLoad()
  }

  function handleSaved(action: 'created' | 'updated', message?: string) {
    setEditor(null)
    setNotice(message ?? (action === 'created' ? 'Contact added.' : 'Contact updated.'))
    refreshAfterChange()
  }

  function handleDeleted(name: string) {
    setEditor(null)
    setNotice(`${name} deleted.`)
    refreshAfterChange()
  }

  return (
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
            <p className="section-label">Directory</p>
            <h2 id="directory-title">Contacts</h2>
          </div>
          <div className="directory-actions">
            {status === 'success' && (
              <span className="result-count">{contacts.length} {itemLabel}</span>
            )}
            <a className="contact-secondary-button" href={contactsExportUrl(search)}>
              Export CSV
            </a>
            {user ? (
              <button className="contact-primary-button" type="button"
                onClick={() => { setEditor({ mode: 'create' }); setNotice(null) }}>
                Add contact
              </button>
            ) : authStatus !== 'loading' ? (
              <Link className="directory-signin" to="/login">Sign in to add</Link>
            ) : null}
          </div>
        </div>

        {notice && <p className="contact-notice" role="status">{notice}</p>}
        {editor && user && (
          <ContactForm key={editor.mode === 'edit' ? editor.contact.id : 'new'}
            contact={editor.mode === 'edit' ? editor.contact : undefined}
            onSaved={handleSaved} onCancel={() => setEditor(null)} />
        )}

        <ContactSearch value={search} onChange={changeSearch} />
        <ContactList
          contacts={contacts}
          status={status}
          isSearching={isSearching}
          onRetry={retryLoad}
          onClearSearch={clearSearch}
          signedIn={user !== null}
          onEdit={(contact) => { setEditor({ mode: 'edit', contact }); setNotice(null) }}
          onDeleted={handleDeleted}
        />
      </section>
    </main>
  )
}
