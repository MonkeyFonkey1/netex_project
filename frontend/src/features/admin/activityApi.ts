export type SignupActivity = {
  userId: number
  email: string
  signedUpAt: string
  processedAt: string
}

export type ContactActivity = {
  eventId: string
  contactId: number
  actorUserId: number
  action: 'CREATED' | 'UPDATED' | 'DELETED'
  occurredAt: string
  processedAt: string
}

export type ActivityHistory = {
  signups: SignupActivity[]
  contactChanges: ContactActivity[]
}

export async function getActivityHistory(signal: AbortSignal): Promise<ActivityHistory> {
  let response: Response
  try {
    response = await fetch('/api/admin/activities', {
      credentials: 'same-origin',
      signal,
    })
  } catch (error) {
    if (signal.aborted) throw error
    throw new Error('Cannot reach the server. Please try again.')
  }

  if (response.status === 503) throw new Error('Activity service is unavailable. Please try again.')
  if (response.status === 401 || response.status === 403) {
    if (response.status === 401) window.dispatchEvent(new Event('netex-session-expired'))
    throw new Error('Your session cannot access this page. Sign in as an admin.')
  }
  if (!response.ok) throw new Error(`Could not load activity: HTTP ${response.status}`)
  return await response.json() as ActivityHistory
}
