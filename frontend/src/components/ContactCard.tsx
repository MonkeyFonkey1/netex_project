import type { Contact } from '../api/contacts'

type ContactCardProps = {
  contact: Contact
}

export function ContactCard({ contact }: ContactCardProps) {
  return (
    <li className="contact-card">
      <span className="contact-avatar" aria-hidden="true">
        {contact.name.charAt(0).toLocaleUpperCase()}
      </span>
      <div className="contact-details">
        <h3>{contact.name}</h3>
        <p>{contact.address}</p>
      </div>
    </li>
  )
}
