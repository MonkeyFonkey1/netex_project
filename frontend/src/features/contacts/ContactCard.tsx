import { useState } from 'react'
import { deleteContact } from './contactsApi'
import type { Contact } from './contactsApi'

type ContactCardProps = {
  contact: Contact
  canManage: boolean
  onEdit: (contact: Contact) => void
  onDeleted: (name: string) => void
}

export function ContactCard({ contact, canManage, onEdit, onDeleted }: ContactCardProps) {
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete() {
    setBusy(true)
    setError(null)
    try {
      await deleteContact(contact.id)
      onDeleted(contact.name)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not delete the contact.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <li className="contact-card">
      <span className="contact-avatar" aria-hidden="true">
        {contact.pictureUrl
          ? <img src={contact.pictureUrl} alt="" />
          : contact.name.charAt(0).toLocaleUpperCase()}
      </span>
      <div className="contact-details">
        <h3>{contact.name}</h3>
        <p>{contact.address}</p>
        {canManage && (
          <div className="contact-card-actions">
            {confirming ? (
              <>
                <span>Delete this contact?</span>
                <button type="button" onClick={handleDelete} disabled={busy}>
                  {busy ? 'Deleting…' : 'Yes, delete'}
                </button>
                <button type="button" onClick={() => setConfirming(false)} disabled={busy}>Cancel</button>
              </>
            ) : (
              <>
                <button type="button" onClick={() => onEdit(contact)}>Edit</button>
                <button type="button" onClick={() => setConfirming(true)}>Delete</button>
              </>
            )}
          </div>
        )}
        {error && <p className="contact-action-error" role="alert">{error}</p>}
      </div>
    </li>
  )
}
