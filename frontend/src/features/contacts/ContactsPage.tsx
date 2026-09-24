import { ContactList } from './ContactList'
import { ContactSearch } from './ContactSearch'
import { useContacts } from './useContacts'
import './contacts.css'

export function ContactsPage() {
  const { search, contacts, status, changeSearch, clearSearch, retryLoad } = useContacts()
  const isSearching = search.trim().length > 0
  const itemLabel = isSearching
    ? (contacts.length === 1 ? 'result' : 'results')
    : (contacts.length === 1 ? 'contact' : 'contacts')

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
          {status === 'success' && (
            <span className="result-count">{contacts.length} {itemLabel}</span>
          )}
        </div>

        <ContactSearch value={search} onChange={changeSearch} />
        <ContactList
          contacts={contacts}
          status={status}
          isSearching={isSearching}
          onRetry={retryLoad}
          onClearSearch={clearSearch}
        />
      </section>
    </main>
  )
}
