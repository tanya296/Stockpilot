import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../lib/api'
import type { DashboardSummary, LowStockItem } from '../lib/types'
import { Card, ErrorText, Loading } from '../components/ui'

function Stat({ label, value, tone = 'text-slate-900' }: { label: string; value: number; tone?: string }) {
  return (
    <Card>
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-1 text-3xl font-semibold ${tone}`}>{value}</p>
    </Card>
  )
}

export function DashboardPage() {
  // TanStack Query: fetches, caches by key, and refetches when the key is invalidated after a change.
  const summary = useQuery({ queryKey: ['dashboard', 'summary'], queryFn: () => api<DashboardSummary>('/api/dashboard/summary') })
  const lowStock = useQuery({ queryKey: ['dashboard', 'low-stock'], queryFn: () => api<LowStockItem[]>('/api/dashboard/low-stock') })

  if (summary.isPending) return <Loading />
  if (summary.isError) return <ErrorText error={summary.error} />
  const s = summary.data

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Dashboard</h1>

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Products" value={s.totalProducts} />
        <Stat label="Low on stock" value={s.lowStockCount} tone={s.lowStockCount > 0 ? 'text-rose-600' : 'text-emerald-600'} />
        <Stat label="Open purchase orders" value={s.openPurchaseOrders} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <h2 className="mb-3 font-medium">Units per warehouse</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={s.stockByWarehouse}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="warehouseName" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="totalUnits" name="Units" fill="#4f46e5" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <h2 className="mb-3 font-medium">Below reorder level</h2>
          {lowStock.isPending ? <Loading /> : lowStock.data?.length === 0 ? (
            <p className="text-sm text-slate-500">Everything is above its reorder level.</p>
          ) : (
            <table className="w-full text-sm">
              <thead className="text-left text-slate-500">
                <tr><th className="py-1">Product</th><th>In stock</th><th>Reorder at</th><th>Short by</th></tr>
              </thead>
              <tbody>
                {lowStock.data?.map((item) => (
                  <tr key={item.productId} className="border-t">
                    <td className="py-2">
                      <Link to={`/products/${item.productId}`} className="text-indigo-600 hover:underline">{item.name}</Link>
                      <div className="text-xs text-slate-400">{item.sku}</div>
                    </td>
                    <td>{item.totalQuantity}</td>
                    <td>{item.reorderLevel}</td>
                    <td className="font-medium text-rose-600">{item.shortfall}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>
    </div>
  )
}
