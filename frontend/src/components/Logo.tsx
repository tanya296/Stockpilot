import { useId } from 'react'
import { motion } from 'framer-motion'

// StockPilot logo: a gradient tile with an isometric box (a "stock" crate).
// The three faces drop in one after another when the logo first appears.
const faces = [
  { d: 'M16 7 L25 11.5 L16 16 L7 11.5 Z', opacity: 1 },      // top
  { d: 'M7 11.5 L16 16 L16 25.5 L7 21 Z', opacity: 0.75 },   // left
  { d: 'M25 11.5 L16 16 L16 25.5 L25 21 Z', opacity: 0.5 },  // right
]

export function LogoMark({ size = 32 }: { size?: number }) {
  // Unique gradient id per logo, so two logos on one page don't clash.
  const gradientId = useId()
  return (
    <motion.svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      whileHover={{ rotate: -8, scale: 1.08 }}
      transition={{ type: 'spring', stiffness: 300, damping: 15 }}
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#6366f1" />
          <stop offset="100%" stopColor="#a855f7" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="9" fill={`url(#${gradientId})`} />
      {faces.map((f, i) => (
        <motion.path
          key={i}
          d={f.d}
          fill="white"
          fillOpacity={f.opacity}
          initial={{ y: -6, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.15 + i * 0.12, type: 'spring', stiffness: 260, damping: 18 }}
        />
      ))}
    </motion.svg>
  )
}

export function Logo({ light = false }: { light?: boolean }) {
  return (
    <div className="flex items-center gap-2.5">
      <LogoMark />
      <span className={`text-lg font-bold tracking-tight ${light ? 'text-white' : 'text-slate-900'}`}>
        Stock<span className={light ? 'text-indigo-200' : 'text-gradient'}>Pilot</span>
      </span>
    </div>
  )
}
