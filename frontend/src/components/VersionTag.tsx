import { GitCommitHorizontal } from 'lucide-react'
import { shortCommit, useAppInfo } from '../lib/useAppInfo'

// Small "v1.0.0 · a1b2c3d" line with the release note, shown on the login page and in the sidebar.
export function VersionTag({ className = '' }: { className?: string }) {
  const { data } = useAppInfo()
  if (!data) return null
  return (
    <div className={`text-xs text-slate-400 ${className}`} title={data.releaseNote}>
      <span className="inline-flex items-center gap-1">
        <GitCommitHorizontal size={12} />
        v{data.version} · {shortCommit(data.commit)}
      </span>
      <p className="mt-0.5 truncate">{data.releaseNote}</p>
    </div>
  )
}
