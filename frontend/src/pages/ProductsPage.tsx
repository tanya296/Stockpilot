import { useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Plus, Search } from 'lucide-react'
import { api } from '../lib/api'
import type { Page, Product } from '../lib/types'
import { money } from '../lib/format'
import { useAuth } from '../auth/AuthContext'
import { ProductFormDialog } from '../components/ProductFormDialog'
import { Badge, btn, Card, ErrorText, input, Loading, PageHeader, Pagination, rowIn } from '../components/ui'

type SortKey = 'name' | 'sku' | 'category' | 'unitPrice'

export function ProductsPage() {
  const { hasRole } = useAuth()
  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState<{ key: SortKey; dir: 'asc' | 'desc' }>({ key: 'name', dir: 'asc' })
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Product | 'new' | null>(null)

  // Debounce: wait 300ms after typing stops before searching, instead of one request per key press.
  useEffect(() => {
    const t = setTimeout(() => { setDebounced(search); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [search])

  const params = new URLSearchParams({ page: String(page), size: '10', sort: `${sort.key},${sort.dir}` })
  if (debounced) params.set('search', debounced)
  if (category) params.set('category', category)

  const products = useQuery({
    queryKey: ['products', debounced, category, sort, page],
    queryFn: () => api<Page<Product>>(`/api/products?${params}`),
    placeholderData: keepPreviousData, // keep showing the old page while the next one loads
  })
  const categories = useQuery({ queryKey: ['categories'], queryFn: () => api<string[]>('/api/products/categories') })

  const toggleSort = (key: SortKey) => {
    setSort((s) => ({ key, dir: s.key === key && s.dir === 'asc' ? 'desc' : 'asc' }))
    setPage(0)
  }
  const Th = ({ k, label }: { k: SortKey; label: string }) => (
    <th className="cursor-pointer select-none py-2 hover:text-slate-900" onClick={() => toggleSort(k)}>
      {label} {sort.key === k ? (sort.dir === 'asc' ? '▲' : '▼') : ''}
    </th>
  )

  return (
    <div className="space-y-4">
      <PageHeader title="Products" subtitle="Everything you stock, across all warehouses."
        action={hasRole('ADMIN') && <button className={btn.primary} onClick={() => setEditing('new')}><Plus size={16} />Add product</button>} />

      <Card>
        <div className="mb-3 flex flex-wrap gap-2">
          <div className="relative w-full max-w-xs">
            <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input className={`${input} pl-9`} placeholder="Search name or SKU…" value={search}
              onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className={`${input} max-w-[200px]`} value={category}
            onChange={(e) => { setCategory(e.target.value); setPage(0) }}>
            <option value="">All categories</option>
            {categories.data?.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        {products.isPending ? <Loading /> : products.isError ? <ErrorText error={products.error} /> : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-xs uppercase tracking-wide text-slate-400">
                  <tr>
                    <Th k="sku" label="SKU" />
                    <Th k="name" label="Name" />
                    <Th k="category" label="Category" />
                    <Th k="unitPrice" label="Price" />
                    <th>In stock</th>
                    <th>Status</th>
                    {hasRole('ADMIN') && <th />}
                  </tr>
                </thead>
                <tbody>
                  {products.data.content.map((p, i) => (
                    <motion.tr key={p.id} className="border-t border-slate-100 transition-colors hover:bg-indigo-50/40" {...rowIn(i)}>
                      <td className="py-3 font-mono text-xs text-slate-500">{p.sku}</td>
                      <td><Link to={`/products/${p.id}`} className="font-medium text-indigo-600 hover:underline">{p.name}</Link></td>
                      <td>{p.category}</td>
                      <td>{money(p.unitPrice)}</td>
                      <td>{p.totalQuantity}</td>
                      <td>{p.lowStock ? <Badge value="LOW" label="Low" /> : <Badge value="OK" />}</td>
                      {hasRole('ADMIN') && (
                        <td className="text-right">
                          <button className="text-slate-500 hover:text-indigo-600" onClick={() => setEditing(p)}>Edit</button>
                        </td>
                      )}
                    </motion.tr>
                  ))}
                  {products.data.content.length === 0 && (
                    <tr><td colSpan={7} className="py-6 text-center text-slate-500">No products match.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={setPage} />
          </>
        )}
      </Card>

      {editing && (
        <ProductFormDialog product={editing === 'new' ? undefined : editing} onClose={() => setEditing(null)} />
      )}
    </div>
  )
}
