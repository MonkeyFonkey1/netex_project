type HealthResponse = { status: string }

export async function getApiHealth(signal: AbortSignal): Promise<HealthResponse> {
  const response = await fetch('/api/health', { signal })

  if (!response.ok) {
    throw new Error(`Health request failed: HTTP ${response.status}`)
  }

  const data: unknown = await response.json()
  if (
    typeof data !== 'object' ||
    data === null ||
    !('status' in data) ||
    typeof data.status !== 'string'
  ) {
    throw new Error('The server returned an invalid health response')
  }

  return { status: data.status }
}
