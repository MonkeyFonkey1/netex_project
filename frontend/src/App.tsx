import { BrowserRouter } from 'react-router'
import { AppRoutes } from './navigation/AppRoutes'
import { SiteHeader } from './navigation/SiteHeader'
import './App.css'

export default function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <SiteHeader />
        <AppRoutes />
        <footer className="site-footer">Netex address book</footer>
      </div>
    </BrowserRouter>
  )
}
