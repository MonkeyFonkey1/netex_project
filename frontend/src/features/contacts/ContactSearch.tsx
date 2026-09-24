type ContactSearchProps = {
  value: string
  onChange: (value: string) => void
}

export function ContactSearch({ value, onChange }: ContactSearchProps) {
  return (
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
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  )
}
