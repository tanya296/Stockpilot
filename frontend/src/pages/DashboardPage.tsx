import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CircleCheck, ClipboardList, Package, TriangleAlert, type LucideIcon } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../lib/api'
import type { DashboardSummary, LowStockItem } from '../lib/types'
import { useAuth } from '../auth/AuthContext'
import { Card, ErrorText, Loading, PageHeader, rowIn } from '../components/ui'
import { AnimatedNumber } from '../components/AnimatedNumber'

// Parent/child "variants": the grid tells each card to appear 0.08s after the previous one.
const grid = { hidden: {}, show: { transition: { staggerChildren: 0.08 } } }
const item = { hidden: { opacity: 0, y: 16 }, show: { opacity: 1, y: 0 } }

function Stat({ label, value, icon: Icon, tone }: { label: string; value: number; icon: LucideIcon; tone: string }) {
  return (
    <motion.div variants={item} whileHover={{ y: -4 }} transition={{ type: 'spring', stiffness: 300, damping: 20 }}>
      <Card className="flex items-center gap-4">
        <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${tone}`}>
          <Icon size={22} />
        </div>
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="text-3xl font-bold tracking-tight"><AnimatedNumber value={value} /></p>
        </div>
      </Card>
    </motion.div>
  )
}

function greeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

export function DashboardPage() {
  const { user } = useAuth()
  // TanStack Query: fetches, caches by key, and refetches when the key is invalidated after a change.
  const summary = useQuery({ queryKey: ['dashboard', 'summary'], queryFn: () => api<DashboardSummary>('/api/dashboard/summary') })
  const lowStock = useQuery({ queryKey: ['dashboard', 'low-stock'], queryFn: () => api<LowStockItem[]>('/api/dashboard/low-stock') })

  if (summary.isPending) return <Loading rows={6} />
  if (summary.isError) return <ErrorText error={summary.error} />
  const s = summary.data
  const low = s.lowStockCount > 0

  return (
    <div className="space-y-6">
      <PageHeader title={`${greeting()}, ${user?.fullName.split(' ')[0] ?? ''}`} subtitle="Here's what's happening across your warehouses." />

      <motion.div className="grid gap-4 sm:grid-cols-3" variants={grid} initial="hidden" animate="show">
        <Stat label="Products" value={s.totalProducts} icon={Package} tone="bg-indigo-50 text-indigo-600" />
        <Stat label="Low on stock" value={s.lowStockCount} icon={low ? TriangleAlert : CircleCheck}
          tone={low ? 'bg-rose-50 text-rose-600' : 'bg-emerald-50 text-emerald-600'} />
        <Stat label="Open purchase orders" value={s.openPurchaseOrders} icon={ClipboardList} tone="bg-violet-50 text-violet-600" />
      </motion.div>

      <motion.div className="grid gap-4 lg:grid-cols-2" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}>
        <Card>
          <h2 className="mb-4 font-semibold">Units per warehouse</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={s.stockByWarehouse}>
                <defs>
                  <linearGradient id="bar" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#8b5cf6" />
                    <stop offset="100%" stopColor="#6366f1" />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="warehouseName" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <Tooltip cursor={{ fill: '#eef2ff' }} contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0' }} />
                <Bar dataKey="totalUnits" name="Units" fill="url(#bar)" radius={[8, 8, 0, 0]} animationDuration={900} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <h2 className="mb-4 font-semibold">Below reorder level</h2>
          {lowStock.isPending ? <Loading /> : lowStock.data?.length === 0 ? (
            <div className="flex flex-col items-center gap-2 py-10 text-sm text-slate-500">
              <CircleCheck className="text-emerald-500" size={32} />
              Everything is above its reorder level.
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead className="text-left text-xs uppercase tracking-wide text-slate-400">
                <tr><th className="py-2">Product</th><th>In stock</th><th>Reorder at</th><th>Short by</th></tr>
              </thead>
              <tbody>
                {lowStock.data?.map((it, i) => (
                  <motion.tr key={it.productId} className="border-t border-slate-100" {...rowIn(i)}>
                    <td className="py-2.5">
                      <Link to={`/products/${it.productId}`} className="font-medium text-indigo-600 hover:underline">{it.name}</Link>
                      <div className="text-xs text-slate-400">{it.sku}</div>
                    </td>
                    <td>{it.totalQuantity}</td>
                    <td>{it.reorderLevel}</td>
                    <td className="font-semibold text-rose-600">{it.shortfall}</td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </motion.div>
    </div>
  )
}
