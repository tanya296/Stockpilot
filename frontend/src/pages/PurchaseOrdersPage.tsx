import { Fragment, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { motion } from 'framer-motion'
import { Plus } from 'lucide-react'
import { api, ApiError, patch } from '../lib/api'
import type { Page, PurchaseOrder, PurchaseOrderStatus } from '../lib/types'
import { dateTime, money } from '../lib/format'
import { useAuth } from '../auth/AuthContext'
import { CreatePurchaseOrderDialog } from '../components/CreatePurchaseOrderDialog'
import { PageHeader, Badge, btn, Card, ErrorText, input, Loading, Pagination, rowIn } from '../components/ui'

const STATUSES: PurchaseOrderStatus[] = ['DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED']

// Which buttons to show for each status (mirrors the backend state machine).
// The backend still validates every transition; this only decides what to display.
const ACTIONS: Record<PurchaseOrderStatus, { to: PurchaseOrderStatus; label: string; style: string }[]> = {
  DRAFT: [
    { to: 'ORDERED', label: 'Place order', style: btn.primary },
    { to: 'CANCELLED', label: 'Cancel', style: btn.secondary },
  ],
  ORDERED: [
    { to: 'RECEIVED', label: 'Receive', style: btn.primary },
    { to: 'CANCELLED', label: 'Cancel', style: btn.secondary },
  ],
  RECEIVED: [],
  CANCELLED: [],
}

export function PurchaseOrdersPage() {
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<PurchaseOrderStatus | ''>('')
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const [expanded, setExpanded] = useState<number | null>(null)

  const orders = useQuery({
    queryKey: ['purchase-orders', status, page],
    queryFn: () => api<Page<PurchaseOrder>>(`/api/purchase-orders?page=${page}&size=10${status ? `&status=${status}` : ''}`),
    placeholderData: keepPreviousData,
  })

  const changeStatus = useMutation({
    mutationFn: ({ id, to }: { id: number; to: PurchaseOrderStatus }) =>
      patch<PurchaseOrder>(`/api/purchase-orders/${id}/status`, { status: to }),
    onSuccess: (po) => {
      toast.success(`PO #${po.id} is now ${po.status.toLowerCase()}`)
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      if (po.status === 'RECEIVED') {
        // Receiving changed stock levels: refresh anything that shows stock.
        queryClient.invalidateQueries({ queryKey: ['products'] })
        queryClient.invalidateQueries({ queryKey: ['product'] })
        queryClient.invalidateQueries({ queryKey: ['movements'] })
      }
    },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Update failed'),
  })

  return (
    <div className="space-y-4">
      <PageHeader title="Purchase orders" subtitle="Order from suppliers; receiving an order adds the stock automatically."
        action={hasRole('MANAGER') && <button className={btn.primary} onClick={() => setCreating(true)}><Plus size={16} />New order</button>} />

      <Card>
        <select className={`${input} mb-3 max-w-[200px]`} value={status}
          onChange={(e) => { setStatus(e.target.value as PurchaseOrderStatus | ''); setPage(0) }}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>

        {orders.isPending ? <Loading /> : orders.isError ? <ErrorText error={orders.error} /> : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-xs uppercase tracking-wide text-slate-400">
                  <tr><th className="py-2">#</th><th>Supplier</th><th>Deliver to</th><th>Total</th><th>Status</th><th>Created</th><th /></tr>
                </thead>
                <tbody>
                  {orders.data.content.map((po, i) => (
                    <Fragment key={po.id}>
                      <motion.tr {...rowIn(i)} className="cursor-pointer border-t border-slate-100 transition-colors hover:bg-indigo-50/40"
                        onClick={() => setExpanded(expanded === po.id ? null : po.id)}>
                        <td className="py-3 font-medium text-slate-500">#{po.id}</td>
                        <td>{po.supplierName}</td>
                        <td>{po.warehouseName}</td>
                        <td>{money(po.totalCost)}</td>
                        <td><Badge value={po.status} /></td>
                        <td className="whitespace-nowrap text-slate-500">{dateTime(po.createdAt)}</td>
                        <td className="space-x-2 whitespace-nowrap text-right" onClick={(e) => e.stopPropagation()}>
                          {hasRole('MANAGER') && ACTIONS[po.status].map((a) => (
                            <button key={a.to} className={`${a.style} px-2 py-1 text-xs`}
                              disabled={changeStatus.isPending}
                              onClick={() => changeStatus.mutate({ id: po.id, to: a.to })}>
                              {a.label}
                            </button>
                          ))}
                        </td>
                      </motion.tr>
                      {expanded === po.id && (
                        <motion.tr className="bg-slate-50/70" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                          <td colSpan={7} className="px-4 py-2">
                            <ul className="space-y-1">
                              {po.items.map((i) => (
                                <li key={i.productId} className="flex justify-between">
                                  <span><span className="font-mono text-xs">{i.sku}</span> {i.productName} × {i.quantity}</span>
                                  <span>{money(i.lineTotal)}</span>
                                </li>
                              ))}
                            </ul>
                            <p className="mt-1 text-xs text-slate-500">Created by {po.createdBy}</p>
                          </td>
                        </motion.tr>
                      )}
                    </Fragment>
                  ))}
                  {orders.data.content.length === 0 && (
                    <tr><td colSpan={7} className="py-6 text-center text-slate-500">No purchase orders yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={orders.data.page} totalPages={orders.data.totalPages} onChange={setPage} />
          </>
        )}
      </Card>

      {creating && <CreatePurchaseOrderDialog onClose={() => setCreating(false)} />}
    </div>
  )
}
