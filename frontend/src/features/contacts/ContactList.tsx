import type { Contact } from './contactsApi'
import type { LoadStatus } from './useContacts'
import { ContactCard } from './ContactCard'

type ContactListProps = {
  contacts: Contact[]
  status: LoadStatus
  isSearching: boolean
  onRetry: () => void
  onClearSearch: () => void
}

export function ContactList({ contacts, status, isSearching, onRetry, onClearSearch }: ContactListProps) {
  return (
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
          <button type="button" onClick={onRetry}>Try again</button>
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
            <button type="button" className="text-button" onClick={onClearSearch}>
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
  )
}
