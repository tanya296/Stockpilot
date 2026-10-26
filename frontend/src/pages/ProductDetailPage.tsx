import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { motion } from 'framer-motion'
import { ArrowLeft, ArrowRightLeft } from 'lucide-react'
import { api, ApiError, post } from '../lib/api'
import type { Movement, Page, ProductDetail, StockLevel, Warehouse } from '../lib/types'
import { dateTime, money } from '../lib/format'
import { useAuth } from '../auth/AuthContext'
import { TransferDialog } from '../components/TransferDialog'
import { Badge, btn, Card, ErrorText, Field, input, Loading, Pagination, rowIn } from '../components/ui'

function AdjustStockForm({ productId }: { productId: number; stock: StockLevel[] }) {
  const queryClient = useQueryClient()
  const { data: warehouses = [] } = useQuery({ queryKey: ['warehouses'], queryFn: () => api<Warehouse[]>('/api/warehouses') })
  const [warehouseId, setWarehouseId] = useState<number | ''>('')
  const [type, setType] = useState<'IN' | 'OUT'>('IN')
  const [quantity, setQuantity] = useState(1)
  const [reason, setReason] = useState('')

  const mutation = useMutation({
    mutationFn: () => post<StockLevel>('/api/stock/adjust', { productId, warehouseId, type, quantity, reason }),
    onSuccess: (level) => {
      toast.success(`${level.warehouseName} now has ${level.quantity}`)
      setQuantity(1)
      setReason('')
      queryClient.invalidateQueries({ queryKey: ['product', productId] })
      queryClient.invalidateQueries({ queryKey: ['movements', productId] })
      queryClient.invalidateQueries({ queryKey: ['products'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    // 422 "Not enough stock", 409 "changed by someone else": show the server's message.
    onError: (e) => toast.error(e instanceof ApiError ? e.message : 'Adjustment failed'),
  })

  return (
    <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); mutation.mutate() }}>
      <h2 className="font-medium">Adjust stock</h2>
      <Field label="Warehouse">
        <select className={input} value={warehouseId} onChange={(e) => setWarehouseId(Number(e.target.value))} required>
          <option value="" disabled>Choose…</option>
          {warehouses.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
        </select>
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Type">
          <select className={input} value={type} onChange={(e) => setType(e.target.value as 'IN' | 'OUT')}>
            <option value="IN">Stock in</option>
            <option value="OUT">Stock out</option>
          </select>
        </Field>
        <Field label="Quantity">
          <input className={input} type="number" min={1} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} required />
        </Field>
      </div>
      <Field label="Reason">
        <input className={input} value={reason} onChange={(e) => setReason(e.target.value)} maxLength={255} placeholder="e.g. used in repair" />
      </Field>
      <button className={`${btn.primary} w-full`} disabled={mutation.isPending}>Save</button>
    </form>
  )
}

export function ProductDetailPage() {
  const id = Number(useParams().id)
  const { hasRole } = useAuth()
  const [page, setPage] = useState(0)
  const [transferOpen, setTransferOpen] = useState(false)

  const detail = useQuery({ queryKey: ['product', id], queryFn: () => api<ProductDetail>(`/api/products/${id}`) })
  const movements = useQuery({
    queryKey: ['movements', id, page],
    queryFn: () => api<Page<Movement>>(`/api/products/${id}/movements?page=${page}&size=10`),
    placeholderData: keepPreviousData,
  })

  if (detail.isPending) return <Loading />
  if (detail.isError) return <ErrorText error={detail.error} />
  const { product, stock } = detail.data

  return (
    <div className="space-y-4">
      <Link to="/products" className="inline-flex items-center gap-1 text-sm text-slate-500 transition hover:-translate-x-0.5 hover:text-slate-900"><ArrowLeft size={14} /> Products</Link>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{product.name}</h1>
          <p className="text-sm text-slate-500">
            <span className="font-mono">{product.sku}</span> · {product.category} · {money(product.unitPrice)} · reorder at {product.reorderLevel}
          </p>
        </div>
        {hasRole('MANAGER') && <button className={btn.secondary} onClick={() => setTransferOpen(true)}><ArrowRightLeft size={16} />Transfer stock</button>}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold">Stock by warehouse</h2>
            <span className="text-sm">
              Total <strong>{product.totalQuantity}</strong>{' '}
              {product.lowStock ? <Badge value="LOW" label="Low" /> : <Badge value="OK" />}
            </span>
          </div>
          <table className="w-full text-sm">
            <thead className="text-left text-xs uppercase tracking-wide text-slate-400"><tr><th className="py-2">Warehouse</th><th>City</th><th className="w-2/5">Share</th><th className="text-right">Quantity</th></tr></thead>
            <tbody>
              {stock.map((s, i) => (
                <motion.tr key={s.warehouseId} className="border-t border-slate-100" {...rowIn(i)}>
                  <td className="py-2.5 font-medium">{s.warehouseName}</td>
                  <td className="text-slate-500">{s.city}</td>
                  <td className="pr-4">
                    {/* Bar grows to this warehouse's share of the total stock. */}
                    <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                      <motion.div className="h-full rounded-full bg-linear-to-r from-indigo-500 to-violet-500"
                        initial={{ width: 0 }}
                        animate={{ width: `${product.totalQuantity ? (s.quantity / product.totalQuantity) * 100 : 0}%` }}
                        transition={{ duration: 0.8, ease: 'easeOut', delay: 0.2 + i * 0.08 }} />
                    </div>
                  </td>
                  <td className="text-right font-semibold">{s.quantity}</td>
                </motion.tr>
              ))}
              {stock.length === 0 && <tr><td colSpan={4} className="py-4 text-center text-slate-500">Not stocked anywhere yet.</td></tr>}
            </tbody>
          </table>
        </Card>
        <Card><AdjustStockForm productId={id} stock={stock} /></Card>
      </div>

      <Card>
        <h2 className="mb-3 font-semibold">Movement history</h2>
        {movements.isPending ? <Loading /> : movements.isError ? <ErrorText error={movements.error} /> : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-xs uppercase tracking-wide text-slate-400">
                  <tr><th className="py-1">When</th><th>Type</th><th>Qty</th><th>Warehouse</th><th>Reason</th><th>By</th></tr>
                </thead>
                <tbody>
                  {movements.data.content.map((m, i) => (
                    <motion.tr key={m.id} className="border-t border-slate-100" {...rowIn(i)}>
                      <td className="whitespace-nowrap py-2.5 text-slate-500">{dateTime(m.createdAt)}</td>
                      <td><Badge value={m.type} /></td>
                      <td>{m.type === 'OUT' || m.type === 'TRANSFER_OUT' ? '−' : '+'}{m.quantity}</td>
                      <td>{m.warehouseName}</td>
                      <td className="text-slate-600">{m.reason}</td>
                      <td className="text-slate-500">{m.createdBy}</td>
                    </motion.tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={movements.data.page} totalPages={movements.data.totalPages} onChange={setPage} />
          </>
        )}
      </Card>

      {transferOpen && (
        <TransferDialog productId={id} productName={product.name} stock={stock} onClose={() => setTransferOpen(false)} />
      )}
    </div>
  )
}
