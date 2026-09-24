import { useEffect, useState } from 'react'
import { getContacts, type Contact } from './contactsApi'

export type LoadStatus = 'loading' | 'success' | 'error'

export function useContacts() {
  const [search, setSearch] = useState('')
  const [contacts, setContacts] = useState<Contact[]>([])
  const [status, setStatus] = useState<LoadStatus>('loading')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const name = search.trim()
    // Wait while the user types, then cancel older requests if the search changes.
    const timer = window.setTimeout(() => {
      getContacts(name, controller.signal)
        .then((result) => {
          if (controller.signal.aborted) return
          setContacts(result)
          setStatus('success')
        })
        .catch(() => {
          if (!controller.signal.aborted) setStatus('error')
        })
    }, name ? 250 : 0)

    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [search, retry])

  function changeSearch(value: string) {
    setSearch(value)
    setStatus('loading')
  }

  function retryLoad() {
    setStatus('loading')
    setRetry((value) => value + 1)
  }

  return {
    search,
    contacts,
    status,
    changeSearch,
    clearSearch: () => changeSearch(''),
    retryLoad,
  }
}
