import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api, ApiError, post } from '../lib/api'
import type { Page, Product, Supplier, Warehouse } from '../lib/types'
import { money } from '../lib/format'
import { btn, Field, input, Modal } from './ui'

interface Line {
  productId: number | ''
  quantity: number
  unitCost: number
}

export function CreatePurchaseOrderDialog({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const { data: suppliers = [] } = useQuery({ queryKey: ['suppliers'], queryFn: () => api<Supplier[]>('/api/suppliers') })
  const { data: warehouses = [] } = useQuery({ queryKey: ['warehouses'], queryFn: () => api<Warehouse[]>('/api/warehouses') })
  const { data: products } = useQuery({
    queryKey: ['products', 'all'],
    queryFn: () => api<Page<Product>>('/api/products?size=200&sort=name'),
  })

  const [supplierId, setSupplierId] = useState<number | ''>('')
  const [warehouseId, setWarehouseId] = useState<number | ''>('')
  const [lines, setLines] = useState<Line[]>([{ productId: '', quantity: 1, unitCost: 0 }])
  const [error, setError] = useState<string | null>(null)

  const total = lines.reduce((sum, l) => sum + l.quantity * l.unitCost, 0)
  const updateLine = (i: number, patch: Partial<Line>) =>
    setLines((ls) => ls.map((l, idx) => (idx === i ? { ...l, ...patch } : l)))

  const mutation = useMutation({
    mutationFn: () => post('/api/purchase-orders', { supplierId, warehouseId, items: lines }),
    onSuccess: () => {
      toast.success('Draft purchase order created')
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Could not create order'),
  })

  return (
    <Modal title="New purchase order" onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          setError(null)
          mutation.mutate()
        }}
      >
        <div className="grid grid-cols-2 gap-3">
          <Field label="Supplier">
            <select className={input} value={supplierId} onChange={(e) => setSupplierId(Number(e.target.value))} required>
              <option value="" disabled>Choose…</option>
              {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Deliver to">
            <select className={input} value={warehouseId} onChange={(e) => setWarehouseId(Number(e.target.value))} required>
              <option value="" disabled>Choose…</option>
              {warehouses.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
            </select>
          </Field>
        </div>

        <div className="space-y-2">
          <span className="text-sm font-medium text-slate-700">Items</span>
          {lines.map((line, i) => (
            <div key={i} className="grid grid-cols-[1fr_70px_100px_auto] gap-2">
              <select className={input} value={line.productId} required
                onChange={(e) => {
                  const p = products?.content.find((x) => x.id === Number(e.target.value))
                  updateLine(i, { productId: Number(e.target.value), unitCost: p?.unitPrice ?? 0 })
                }}>
                <option value="" disabled>Product…</option>
                {products?.content.map((p) => <option key={p.id} value={p.id}>{p.sku} · {p.name}</option>)}
              </select>
              <input className={input} type="number" min={1} value={line.quantity} title="Quantity"
                onChange={(e) => updateLine(i, { quantity: Number(e.target.value) })} required />
              <input className={input} type="number" min={0} step="0.01" value={line.unitCost} title="Unit cost"
                onChange={(e) => updateLine(i, { unitCost: Number(e.target.value) })} required />
              <button type="button" className="px-2 text-slate-400 hover:text-rose-600" aria-label="Remove line"
                disabled={lines.length === 1} onClick={() => setLines((ls) => ls.filter((_, idx) => idx !== i))}>
                ✕
              </button>
            </div>
          ))}
          <button type="button" className="text-sm text-indigo-600 hover:underline"
            onClick={() => setLines((ls) => [...ls, { productId: '', quantity: 1, unitCost: 0 }])}>
            + Add line
          </button>
        </div>

        <p className="text-right text-sm font-medium">Total: {money(total)}</p>
        {error && <p className="rounded-md bg-rose-50 p-2 text-sm text-rose-700">{error}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className={btn.secondary} onClick={onClose}>Cancel</button>
          <button className={btn.primary} disabled={mutation.isPending}>Create draft</button>
        </div>
      </form>
    </Modal>
  )
}
