import { useEffect, useState } from 'react'
import { animate } from 'framer-motion'

// Counts up from the previous value to the new one, e.g. 0 → 42 on the dashboard.
export function AnimatedNumber({ value }: { value: number }) {
  const [display, setDisplay] = useState(0)

  useEffect(() => {
    const controls = animate(display, value, {
      duration: 1,
      ease: 'easeOut',
      onUpdate: (v) => setDisplay(Math.round(v)),
    })
    return () => controls.stop()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value])

  return <>{display.toLocaleString()}</>
}
