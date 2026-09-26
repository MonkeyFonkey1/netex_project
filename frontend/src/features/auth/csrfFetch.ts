let token: string | null = null
let pendingToken: Promise<string> | null = null

export function clearCsrfToken() {
  token = null
}

async function getCsrfToken(): Promise<string> {
  if (token) return token
  if (!pendingToken) {
    pendingToken = fetch('/api/auth/csrf', { credentials: 'same-origin' })
      .then(async (response) => {
        if (!response.ok) throw new Error(`Could not get CSRF token: HTTP ${response.status}`)
        const csrf = await response.json() as { token: string }
        token = csrf.token
        return csrf.token
      })
      .finally(() => { pendingToken = null })
  }
  return pendingToken
}

export async function csrfFetch(path: string, options: RequestInit): Promise<Response> {
  const currentToken = await getCsrfToken()
  const send = (csrfToken: string) => {
    const headers = new Headers(options.headers)
    headers.set('X-CSRF-TOKEN', csrfToken)
    return fetch(path, { credentials: 'same-origin', ...options, headers })
  }

  let response = await send(currentToken)
  if (response.status === 403) {
    // A restarted or expired session can leave the browser holding an old token.
    clearCsrfToken()
    const freshToken = await getCsrfToken()
    if (freshToken !== currentToken) response = await send(freshToken)
  }
  return response
}
