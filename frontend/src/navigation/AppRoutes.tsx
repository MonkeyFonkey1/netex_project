import { Route, Routes } from 'react-router'
import { AdminActivityPage } from '../features/admin/AdminActivityPage'
import { LoginPage } from '../features/auth/LoginPage'
import { SignupPage } from '../features/auth/SignupPage'
import { ContactsPage } from '../features/contacts/ContactsPage'
import { NotFoundPage } from './NotFoundPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<ContactsPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/admin/activity" element={<AdminActivityPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
