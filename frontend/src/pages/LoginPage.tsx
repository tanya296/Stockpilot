import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRightLeft, ClipboardCheck, ShieldCheck, Shield, UserCog, User } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'
import { btn, Field, input } from '../components/ui'
import { Logo } from '../components/Logo'

const DEMO = [
  { email: 'admin@stockpilot.dev', label: 'Admin', icon: Shield },
  { email: 'manager@stockpilot.dev', label: 'Manager', icon: UserCog },
  { email: 'staff@stockpilot.dev', label: 'Staff', icon: User },
]

const FEATURES = [
  { icon: ArrowRightLeft, text: 'Move stock between warehouses safely' },
  { icon: ClipboardCheck, text: 'Raise and receive purchase orders' },
  { icon: ShieldCheck, text: 'Role-based access for every team member' },
]

// Little translucent boxes that float up and down behind the brand panel.
const CUBES = [
  { size: 64, left: '14%', top: '20%', delay: 0 },
  { size: 40, left: '74%', top: '12%', delay: 0.8 },
  { size: 96, left: '62%', top: '32%', delay: 1.6 },
  { size: 32, left: '38%', top: '42%', delay: 0.4 },
]

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
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Left: brand panel (hidden on phones) */}
      <div className="relative hidden overflow-hidden bg-linear-to-br from-indigo-600 via-violet-600 to-fuchsia-600 p-12 text-white lg:flex lg:flex-col">
        {CUBES.map((c, i) => (
          <motion.div
            key={i}
            className="absolute rounded-2xl border border-white/20 bg-white/10 backdrop-blur-sm"
            style={{ width: c.size, height: c.size, left: c.left, top: c.top }}
            animate={{ y: [0, -18, 0], rotate: [0, 8, 0] }}
            transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut', delay: c.delay }}
          />
        ))}
        <div className="relative"><Logo light /></div>
        <div className="relative mt-auto max-w-md">
          <motion.h2
            className="text-4xl font-bold leading-tight"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            Inventory across every warehouse, in one place.
          </motion.h2>
          <ul className="mt-8 space-y-3">
            {FEATURES.map((f, i) => (
              <motion.li
                key={f.text}
                className="flex items-center gap-3 text-indigo-50"
                initial={{ opacity: 0, x: -16 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3 + i * 0.12 }}
              >
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/15"><f.icon size={16} /></span>
                {f.text}
              </motion.li>
            ))}
          </ul>
        </div>
      </div>

      {/* Right: the form */}
      <div className="flex items-center justify-center p-6">
        <motion.div
          className="w-full max-w-sm"
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
        >
          <div className="mb-8 lg:hidden"><Logo /></div>
          <h1 className="text-2xl font-bold tracking-tight">Welcome back</h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">Sign in to manage your inventory.</p>

          <form onSubmit={submit} className="space-y-4">
            <Field label="Email">
              <input className={input} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
            </Field>
            <Field label="Password">
              <input className={input} type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </Field>
            {error && (
              // A little shake when login fails.
              <motion.p
                key={error}
                className="rounded-lg bg-rose-50 p-2.5 text-sm text-rose-700"
                initial={{ x: 0 }}
                animate={{ x: [0, -8, 8, -5, 5, 0] }}
                transition={{ duration: 0.4 }}
              >
                {error}
              </motion.p>
            )}
            <button className={`${btn.primary} w-full py-2.5`} disabled={busy}>
              {busy && <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />}
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="mt-8 rounded-xl border border-dashed border-slate-300 p-4">
            <p className="mb-3 text-xs text-slate-500">
              Try a demo account (password <code className="rounded bg-slate-100 px-1">Password@123</code>):
            </p>
            <div className="grid grid-cols-3 gap-2">
              {DEMO.map((d) => (
                <motion.button
                  key={d.email}
                  type="button"
                  whileHover={{ y: -2 }}
                  whileTap={{ scale: 0.96 }}
                  className={`flex flex-col items-center gap-1 rounded-lg border px-2 py-2 text-xs font-medium transition ${email === d.email ? 'border-indigo-300 bg-indigo-50 text-indigo-700' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'}`}
                  onClick={() => { setEmail(d.email); setPassword('Password@123') }}
                >
                  <d.icon size={16} />
                  {d.label}
                </motion.button>
              ))}
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}
