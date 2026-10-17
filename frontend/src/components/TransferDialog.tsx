import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api, ApiError, post } from '../lib/api'
import type { StockLevel, Warehouse } from '../lib/types'
import { btn, Field, input, Modal } from './ui'

interface Props {
  productId: number
  productName: string
  stock: StockLevel[]
  onClose: () => void
}

export function TransferDialog({ productId, productName, stock, onClose }: Props) {
  const queryClient = useQueryClient()
  const { data: warehouses = [] } = useQuery({
    queryKey: ['warehouses'],
    queryFn: () => api<Warehouse[]>('/api/warehouses'),
  })

  const [fromId, setFromId] = useState<number | ''>(stock.find((s) => s.quantity > 0)?.warehouseId ?? '')
  const [toId, setToId] = useState<number | ''>('')
  const [quantity, setQuantity] = useState(1)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const available = stock.find((s) => s.warehouseId === fromId)?.quantity ?? 0

  const mutation = useMutation({
    mutationFn: () =>
      post('/api/stock/transfer', { productId, fromWarehouseId: fromId, toWarehouseId: toId, quantity, reason }),
    onSuccess: () => {
      toast.success(`Moved ${quantity} units`)
      // Refetch everything that shows stock so every screen is up to date.
      queryClient.invalidateQueries({ queryKey: ['product', productId] })
      queryClient.invalidateQueries({ queryKey: ['movements', productId] })
      queryClient.invalidateQueries({ queryKey: ['products'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    // The server is the source of truth: show its message (e.g. "Not enough stock ...", or 409 on a conflict).
    onError: (e) => setError(e instanceof ApiError ? e.message : 'Transfer failed'),
  })

  return (
    <Modal title={`Transfer ${productName}`} onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          setError(null)
          mutation.mutate()
        }}
      >
        <Field label={`From (${available} available)`}>
          <select className={input} value={fromId} onChange={(e) => setFromId(Number(e.target.value))} required>
            <option value="" disabled>Choose…</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
        </Field>
        <Field label="To">
          <select className={input} value={toId} onChange={(e) => setToId(Number(e.target.value))} required>
            <option value="" disabled>Choose…</option>
            {warehouses.filter((w) => w.id !== fromId).map((w) => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
        </Field>
        <Field label="Quantity">
          <input className={input} type="number" min={1} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} required />
        </Field>
        <Field label="Reason (optional)">
          <input className={input} value={reason} onChange={(e) => setReason(e.target.value)} maxLength={255} />
        </Field>
        {error && <p className="rounded-md bg-rose-50 p-2 text-sm text-rose-700">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className={btn.secondary} onClick={onClose}>Cancel</button>
          <button className={btn.primary} disabled={mutation.isPending}>
            {mutation.isPending ? 'Moving…' : 'Transfer'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
