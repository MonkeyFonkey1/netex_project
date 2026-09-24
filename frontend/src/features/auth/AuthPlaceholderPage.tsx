import { Link } from 'react-router'

type AuthPlaceholderPageProps = {
  mode: 'login' | 'signup'
}

export function AuthPlaceholderPage({ mode }: AuthPlaceholderPageProps) {
  const isLogin = mode === 'login'

  return (
    <main className="placeholder-page">
      <p className="eyebrow">Account access</p>
      <h1>{isLogin ? 'Sign in' : 'Create an account'}</h1>
      <p>
        {isLogin
          ? 'Sign in will be available when account access is added.'
          : 'Registration will be available when account access is added.'}
      </p>
      <Link className="page-link" to="/">View public contacts</Link>
    </main>
  )
}
