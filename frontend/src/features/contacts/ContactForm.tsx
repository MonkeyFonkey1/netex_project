import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  createContact, removeContactPicture, updateContact, uploadContactPicture,
  type Contact,
} from './contactsApi'

type ContactFormProps = {
  contact?: Contact
  onSaved: (action: 'created' | 'updated', message?: string) => void
  onCancel: () => void
}

export function ContactForm({ contact, onSaved, onCancel }: ContactFormProps) {
  const [name, setName] = useState(contact?.name ?? '')
  const [address, setAddress] = useState(contact?.address ?? '')
  const [picture, setPicture] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [removePicture, setRemovePicture] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)
  const editing = contact !== undefined

  useEffect(() => {
    return () => { if (previewUrl) URL.revokeObjectURL(previewUrl) }
  }, [previewUrl])

  function clearSelection() {
    setPicture(null)
    setPreviewUrl(null)
    if (fileInput.current) fileInput.current.value = ''
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const input = { name: name.trim(), address: address.trim() }
    if (!input.name || !input.address) {
      setError('Enter both a name and an address.')
      return
    }
    if (picture && picture.size > 5 * 1024 * 1024) {
      setError('The picture must be smaller than 5 MB.')
      return
    }

    setBusy(true)
    try {
      if (contact) {
        const textChanged = input.name !== contact.name || input.address !== contact.address
        if (textChanged) await updateContact(contact.id, input)
        try {
          if (picture) await uploadContactPicture(contact.id, picture)
          else if (removePicture) await removeContactPicture(contact.id)
        } catch (cause) {
          if (!textChanged) throw cause
          onSaved('updated', 'Contact details saved, but the picture change failed. Edit it to retry.')
          return
        }
        onSaved('updated')
      } else {
        const created = await createContact(input)
        if (picture) {
          try {
            await uploadContactPicture(created.id, picture)
          } catch {
            onSaved('created', 'Contact added, but the picture upload failed. Edit it to retry.')
            return
          }
        }
        onSaved('created')
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save the contact.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="contact-form" onSubmit={handleSubmit}>
      <div className="contact-form-heading">
        <h3>{editing ? 'Edit contact' : 'Add a contact'}</h3>
        <p>Add a JPEG or PNG photo, up to 5 MB.</p>
      </div>

      <label htmlFor="contact-name">Name</label>
      <input id="contact-name" value={name} onChange={(event) => setName(event.target.value)}
        maxLength={255} required autoFocus />

      <label htmlFor="contact-address">Address</label>
      <textarea id="contact-address" value={address}
        onChange={(event) => setAddress(event.target.value)}
        maxLength={1000} rows={3} required />

      <label htmlFor="contact-picture">Photo (optional)</label>
      <div className="contact-photo-controls">
        <div className="contact-photo-preview" aria-hidden="true">
          {(previewUrl || (!removePicture && contact?.pictureUrl))
            ? <img src={previewUrl || contact?.pictureUrl || ''} alt="" />
            : <span>{name.trim().charAt(0).toLocaleUpperCase() || '?'}</span>}
        </div>
        <div className="contact-photo-input">
          <input id="contact-picture" ref={fileInput} type="file" accept="image/jpeg,image/png"
            onChange={(event) => {
              const selected = event.target.files?.[0] ?? null
              setPicture(selected)
              setPreviewUrl(selected ? URL.createObjectURL(selected) : null)
              setRemovePicture(false)
              setError(null)
            }} />
          {picture && <button type="button" onClick={clearSelection} disabled={busy}>Clear selection</button>}
          {editing && contact.pictureUrl && !picture && (
            <button type="button" disabled={busy} onClick={() => setRemovePicture(!removePicture)}>
              {removePicture ? 'Keep current photo' : 'Remove current photo'}
            </button>
          )}
        </div>
      </div>
      {removePicture && <p className="contact-photo-note">The photo will be removed when you save.</p>}

      {error && <p className="contact-action-error" role="alert">{error}</p>}
      <div className="contact-form-actions">
        <button className="contact-primary-button" type="submit" disabled={busy}>
          {busy ? 'Saving…' : editing ? 'Save changes' : 'Add contact'}
        </button>
        <button className="contact-secondary-button" type="button" onClick={onCancel} disabled={busy}>
          Cancel
        </button>
      </div>
    </form>
  )
}
