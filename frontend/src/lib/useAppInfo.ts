import { useQuery } from '@tanstack/react-query'
import { api } from './api'

export interface AppInfo {
  app: string
  version: string
  commit: string
  releaseNote: string
}

// GET /api/info is public. Showing it in the UI makes each deployment visible:
// after a push, the pipeline rolls out a new image and these values change.
export function useAppInfo() {
  return useQuery({
    queryKey: ['app-info'],
    queryFn: () => api<AppInfo>('/api/info'),
    staleTime: 60_000,
    retry: false,
  })
}

export function shortCommit(commit: string) {
  return commit.length > 7 ? commit.slice(0, 7) : commit
}
