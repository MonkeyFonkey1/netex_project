import { Route, Routes } from 'react-router'
import { AdminActivityPlaceholderPage } from '../features/admin/AdminActivityPlaceholderPage'
import { AuthPlaceholderPage } from '../features/auth/AuthPlaceholderPage'
import { ContactsPage } from '../features/contacts/ContactsPage'
import { NotFoundPage } from './NotFoundPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<ContactsPage />} />
      <Route path="/login" element={<AuthPlaceholderPage mode="login" />} />
      <Route path="/signup" element={<AuthPlaceholderPage mode="signup" />} />
      <Route path="/admin/activity" element={<AdminActivityPlaceholderPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
