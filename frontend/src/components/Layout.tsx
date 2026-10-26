import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ClipboardList, LayoutDashboard, LogOut, Package } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { Logo } from './Logo'

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/products', label: 'Products', icon: Package, end: false },
  { to: '/purchase-orders', label: 'Purchase orders', icon: ClipboardList, end: false },
]

// One nav link. The highlighted background is a single shared element (layoutId),
// so Framer Motion slides it smoothly from the old link to the new one.
function NavItem({ to, label, icon: Icon, end, pillId }: (typeof NAV)[number] & { pillId: string }) {
  return (
    <NavLink to={to} end={end} className="relative">
      {({ isActive }) => (
        <span className={`relative z-10 flex items-center gap-2.5 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition-colors ${isActive ? 'text-indigo-700' : 'text-slate-500 hover:text-slate-900'}`}>
          {isActive && (
            <motion.span
              layoutId={pillId}
              className="absolute inset-0 -z-10 rounded-lg bg-indigo-50 ring-1 ring-indigo-100"
              transition={{ type: 'spring', stiffness: 400, damping: 32 }}
            />
          )}
          <Icon size={18} />
          {label}
        </span>
      )}
    </NavLink>
  )
}

function UserChip({ compact = false }: { compact?: boolean }) {
  const { user, logout } = useAuth()
  const initials = user?.fullName.split(' ').map((p) => p[0]).slice(0, 2).join('') ?? '?'
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-linear-to-br from-indigo-500 to-violet-500 text-xs font-semibold text-white">
        {initials}
      </div>
      <div className={`min-w-0 flex-1 text-sm ${compact ? 'hidden sm:block' : ''}`}>
        <p className="truncate font-medium text-slate-900">{user?.fullName}</p>
        <p className="text-xs text-slate-500">{user?.role.toLowerCase()}</p>
      </div>
      <button onClick={logout} title="Log out" aria-label="Log out"
        className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-rose-600">
        <LogOut size={18} />
      </button>
    </div>
  )
}

export function Layout() {
  const location = useLocation()
  return (
    <div className="min-h-screen lg:flex">
      {/* Sidebar on large screens */}
      <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-slate-200/80 bg-white/70 p-4 backdrop-blur lg:flex">
        <div className="px-2 py-2"><Logo /></div>
        <nav className="mt-8 flex flex-col gap-1">
          {NAV.map((n) => <NavItem key={n.to} {...n} pillId="nav-pill-side" />)}
        </nav>
        <div className="mt-auto border-t border-slate-200 pt-4"><UserChip /></div>
      </aside>

      {/* Top bar on phones and small screens */}
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/80 px-4 py-3 backdrop-blur lg:hidden">
        <div className="flex items-center justify-between"><Logo /><UserChip compact /></div>
        <nav className="mt-3 flex gap-1 overflow-x-auto">
          {NAV.map((n) => <NavItem key={n.to} {...n} pillId="nav-pill-top" />)}
        </nav>
      </header>

      {/* key = the URL, so each new page fades and slides in */}
      <motion.main
        key={location.pathname}
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
        className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 lg:px-8 lg:py-8"
      >
        <Outlet />
      </motion.main>
    </div>
  )
}
