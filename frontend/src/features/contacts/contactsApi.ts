export type Contact = {
  id: number
  name: string
  address: string
}

export async function getContacts(name: string, signal: AbortSignal): Promise<Contact[]> {
  const params = new URLSearchParams()
  if (name) params.set('name', name)

  const query = params.toString()
  const response = await fetch(`/api/contacts${query ? `?${query}` : ''}`, { signal })

  if (!response.ok) {
    throw new Error(`Could not load contacts: HTTP ${response.status}`)
  }

  const contacts: unknown = await response.json()
  if (!Array.isArray(contacts)) {
    throw new Error('The server returned an invalid contacts list')
  }

  return contacts as Contact[]
}
