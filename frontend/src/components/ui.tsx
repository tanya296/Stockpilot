import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import { X } from 'lucide-react'

// Tiny shared UI pieces so pages stay readable.

const press = 'transition active:scale-[0.97] disabled:pointer-events-none disabled:opacity-50'

export const btn = {
  primary: `inline-flex items-center justify-center gap-1.5 rounded-lg bg-linear-to-r from-indigo-600 to-violet-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm shadow-indigo-500/30 hover:shadow-md hover:shadow-indigo-500/40 hover:brightness-110 ${press}`,
  secondary: `inline-flex items-center justify-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-sm font-medium text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50 ${press}`,
  danger: `inline-flex items-center justify-center gap-1.5 rounded-lg bg-rose-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm hover:bg-rose-500 ${press}`,
}

export const input =
  'w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm shadow-sm transition placeholder:text-slate-400 focus:border-indigo-400 focus:outline-none focus:ring-4 focus:ring-indigo-500/15'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-2xl border border-slate-200/80 bg-white/80 p-5 shadow-sm backdrop-blur transition-shadow hover:shadow-md ${className}`}>
      {children}
    </div>
  )
}

// Page title + optional subtitle and action button, with a small slide-in.
export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-3">
      <motion.div initial={{ opacity: 0, x: -12 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.35 }}>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
        {subtitle && <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>}
      </motion.div>
      {action}
    </div>
  )
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
    <motion.div
      className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm"
      onClick={onClose}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      {/* The panel springs up slightly as it fades in. */}
      <motion.div
        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        initial={{ opacity: 0, scale: 0.95, y: 16 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ type: 'spring', stiffness: 320, damping: 26 }}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="rounded-md p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600" aria-label="Close">
            <X size={18} />
          </button>
        </div>
        {children}
      </motion.div>
    </motion.div>
  )
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-end gap-2 pt-4 text-sm">
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
  DRAFT: 'bg-slate-100 text-slate-700 ring-slate-200',
  ORDERED: 'bg-blue-50 text-blue-700 ring-blue-200',
  RECEIVED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  CANCELLED: 'bg-rose-50 text-rose-700 ring-rose-200',
  IN: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  PO_RECEIPT: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  TRANSFER_IN: 'bg-sky-50 text-sky-700 ring-sky-200',
  OUT: 'bg-amber-50 text-amber-800 ring-amber-200',
  TRANSFER_OUT: 'bg-violet-50 text-violet-700 ring-violet-200',
  LOW: 'bg-rose-50 text-rose-700 ring-rose-200',
  OK: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
}

export function Badge({ value, label }: { value: string; label?: string }) {
  const pulse = value === 'LOW'
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${BADGE[value] ?? 'bg-slate-100 ring-slate-200'}`}>
      {/* A small dot; it pulses for low stock to draw the eye. */}
      <span className="relative flex h-1.5 w-1.5">
        {pulse && <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-60" />}
        <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
      </span>
      {label ?? value.replace('_', ' ')}
    </span>
  )
}

// Grey shimmering bars instead of the text "Loading…".
export function Loading({ rows = 4 }: { rows?: number }) {
  return (
    <div className="space-y-3 p-2" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="skeleton h-5" style={{ width: `${90 - i * 12}%` }} />
      ))}
    </div>
  )
}

export function ErrorText({ error }: { error: unknown }) {
  return <p className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error instanceof Error ? error.message : 'Something went wrong.'}</p>
}

// Fades and slides table rows in one after another (index = row number).
export const rowIn = (index: number) => ({
  initial: { opacity: 0, y: 6 },
  animate: { opacity: 1, y: 0 },
  transition: { delay: Math.min(index * 0.04, 0.4), duration: 0.25 },
})
