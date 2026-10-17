import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const link = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:text-slate-900'}`

export function Layout() {
  const { user, logout } = useAuth()
  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-2 px-4 py-3">
          <span className="mr-4 text-lg font-bold text-indigo-700">StockPilot</span>
          <nav className="flex flex-wrap gap-1">
            <NavLink to="/" end className={link}>Dashboard</NavLink>
            <NavLink to="/products" className={link}>Products</NavLink>
            <NavLink to="/purchase-orders" className={link}>Purchase orders</NavLink>
          </nav>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="text-slate-600">
              {user?.fullName} <span className="rounded bg-slate-100 px-1.5 py-0.5 text-xs">{user?.role}</span>
            </span>
            <button onClick={logout} className="text-slate-500 hover:text-slate-900">Log out</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
