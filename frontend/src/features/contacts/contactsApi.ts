export type Contact = {
  id: number
  name: string
  address: string
  canManage: boolean
  pictureUrl: string | null
}

export type ContactInput = { name: string; address: string }

function searchQuery(name: string): string {
  const search = name.trim()
  return search ? `?${new URLSearchParams({ name: search })}` : ''
}

export async function getContacts(name: string, signal: AbortSignal): Promise<Contact[]> {
  const response = await fetch(`/api/contacts${searchQuery(name)}`, { signal })

  if (!response.ok) {
    throw new Error(`Could not load contacts: HTTP ${response.status}`)
  }

  const contacts: unknown = await response.json()
  if (!Array.isArray(contacts)) {
    throw new Error('The server returned an invalid contacts list')
  }

  return contacts as Contact[]
}

export function contactsExportUrl(name: string): string {
  return `/api/contacts/export${searchQuery(name)}`
}

async function sendContact(path: string, options: RequestInit): Promise<Response> {
  let response: Response
  try {
    response = await fetch(path, { credentials: 'same-origin', ...options })
  } catch {
    throw new Error('Cannot reach the server. Please try again.')
  }

  if (response.status === 401) throw new Error('Your session has ended. Sign in again.')
  if (response.status === 403) throw new Error('Only the author or an admin can change this contact.')
  if (response.status === 404) throw new Error('This contact no longer exists. Refresh the list.')
  if (response.status === 400) throw new Error('Check the fields and use a valid JPEG or PNG picture.')
  if (response.status === 409) throw new Error('This picture changed. Refresh the list and try again.')
  if (response.status === 413) throw new Error('The picture must be smaller than 5 MB.')
  if (!response.ok) throw new Error(`Contact request failed: HTTP ${response.status}`)
  return response
}

export async function createContact(input: ContactInput): Promise<Contact> {
  const response = await sendContact('/api/contacts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return await response.json() as Contact
}

export async function updateContact(id: number, input: ContactInput): Promise<Contact> {
  const response = await sendContact(`/api/contacts/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return await response.json() as Contact
}

export async function deleteContact(id: number): Promise<void> {
  await sendContact(`/api/contacts/${id}`, { method: 'DELETE' })
}

export async function uploadContactPicture(id: number, picture: File): Promise<void> {
  const data = new FormData()
  data.append('picture', picture)
  await sendContact(`/api/contacts/${id}/picture`, { method: 'PUT', body: data })
}

export async function removeContactPicture(id: number): Promise<void> {
  await sendContact(`/api/contacts/${id}/picture`, { method: 'DELETE' })
}
