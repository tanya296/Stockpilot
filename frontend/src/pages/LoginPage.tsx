import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'
import { btn, Card, Field, input } from '../components/ui'

const DEMO = ['admin@stockpilot.dev', 'manager@stockpilot.dev', 'staff@stockpilot.dev']

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(email, password)
      const from = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reach the server.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-sm p-6">
        <h1 className="text-2xl font-bold text-indigo-700">StockPilot</h1>
        <p className="mb-6 text-sm text-slate-500">Multi-warehouse inventory</p>
        <form onSubmit={submit} className="space-y-4">
          <Field label="Email">
            <input className={input} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </Field>
          <Field label="Password">
            <input className={input} type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>
          {error && <p className="rounded-md bg-rose-50 p-2 text-sm text-rose-700">{error}</p>}
          <button className={`${btn.primary} w-full`} disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </form>
        <div className="mt-6 border-t pt-4 text-xs text-slate-500">
          <p className="mb-2">Demo accounts (password <code>Password@123</code>):</p>
          <div className="flex flex-wrap gap-2">
            {DEMO.map((d) => (
              <button key={d} type="button" className="rounded bg-slate-100 px-2 py-1 hover:bg-slate-200"
                onClick={() => { setEmail(d); setPassword('Password@123') }}>
                {d.split('@')[0]}
              </button>
            ))}
          </div>
        </div>
      </Card>
    </div>
  )
}
