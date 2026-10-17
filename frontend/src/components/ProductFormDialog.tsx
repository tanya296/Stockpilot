import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ApiError, post, put } from '../lib/api'
import type { Product, ProductInput } from '../lib/types'
import { btn, Field, input, Modal } from './ui'

/** Add or edit a product (ADMIN only). Pass `product` to edit. */
export function ProductFormDialog({ product, onClose }: { product?: Product; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<ProductInput>({
    sku: product?.sku ?? '',
    name: product?.name ?? '',
    category: product?.category ?? '',
    unitPrice: product?.unitPrice ?? 0,
    reorderLevel: product?.reorderLevel ?? 0,
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [message, setMessage] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => (product ? put(`/api/products/${product.id}`, form) : post('/api/products', form)),
    onSuccess: () => {
      toast.success(product ? 'Product updated' : 'Product created')
      queryClient.invalidateQueries({ queryKey: ['products'] })
      if (product) queryClient.invalidateQueries({ queryKey: ['product', product.id] })
      onClose()
    },
    onError: (e) => {
      if (e instanceof ApiError) {
        setErrors(e.fieldErrors) // 400: show each field's message under that field
        setMessage(e.message)
      }
    },
  })

  const set = (key: keyof ProductInput, value: string | number) => setForm((f) => ({ ...f, [key]: value }))

  return (
    <Modal title={product ? 'Edit product' : 'Add product'} onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          setErrors({})
          setMessage(null)
          mutation.mutate()
        }}
      >
        <div className="grid grid-cols-2 gap-3">
          <Field label="SKU" error={errors.sku}>
            <input className={input} value={form.sku} onChange={(e) => set('sku', e.target.value)} required />
          </Field>
          <Field label="Category" error={errors.category}>
            <input className={input} value={form.category} onChange={(e) => set('category', e.target.value)} required />
          </Field>
        </div>
        <Field label="Name" error={errors.name}>
          <input className={input} value={form.name} onChange={(e) => set('name', e.target.value)} required />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Unit price (₹)" error={errors.unitPrice}>
            <input className={input} type="number" min={0} step="0.01" value={form.unitPrice}
              onChange={(e) => set('unitPrice', Number(e.target.value))} required />
          </Field>
          <Field label="Reorder level" error={errors.reorderLevel}>
            <input className={input} type="number" min={0} value={form.reorderLevel}
              onChange={(e) => set('reorderLevel', Number(e.target.value))} required />
          </Field>
        </div>
        {message && <p className="rounded-md bg-rose-50 p-2 text-sm text-rose-700">{message}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className={btn.secondary} onClick={onClose}>Cancel</button>
          <button className={btn.primary} disabled={mutation.isPending}>Save</button>
        </div>
      </form>
    </Modal>
  )
}
