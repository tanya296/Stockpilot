import type { ReactNode } from 'react'

// Tiny shared UI pieces so pages stay readable.

export const btn = {
  primary: 'rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50',
  secondary: 'rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50',
  danger: 'rounded-md bg-rose-600 px-3 py-2 text-sm font-medium text-white hover:bg-rose-500 disabled:opacity-50',
}

export const input =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-lg border border-slate-200 bg-white p-4 shadow-sm ${className}`}>{children}</div>
}

export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block space-y-1">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      {children}
      {error && <span className="text-xs text-rose-600">{error}</span>}
    </label>
  )
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4" onClick={onClose}>
      <div
        className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600" aria-label="Close">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-end gap-2 pt-3 text-sm">
      <button className={btn.secondary} disabled={page === 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="text-slate-600">
        Page {page + 1} of {totalPages}
      </span>
      <button className={btn.secondary} disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </div>
  )
}

const BADGE: Record<string, string> = {
  DRAFT: 'bg-slate-100 text-slate-700',
  ORDERED: 'bg-blue-100 text-blue-700',
  RECEIVED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-rose-100 text-rose-700',
  IN: 'bg-emerald-100 text-emerald-700',
  PO_RECEIPT: 'bg-emerald-100 text-emerald-700',
  TRANSFER_IN: 'bg-sky-100 text-sky-700',
  OUT: 'bg-amber-100 text-amber-800',
  TRANSFER_OUT: 'bg-violet-100 text-violet-700',
  LOW: 'bg-rose-100 text-rose-700',
  OK: 'bg-emerald-100 text-emerald-700',
}

export function Badge({ value, label }: { value: string; label?: string }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${BADGE[value] ?? 'bg-slate-100'}`}>
      {label ?? value.replace('_', ' ')}
    </span>
  )
}

export function Loading() {
  return <p className="p-4 text-sm text-slate-500">Loading…</p>
}

export function ErrorText({ error }: { error: unknown }) {
  return <p className="p-4 text-sm text-rose-600">{error instanceof Error ? error.message : 'Something went wrong.'}</p>
}
