// One small wrapper around fetch used by every request:
// - prefixes the backend URL
// - attaches the JWT as "Authorization: Bearer <token>"
// - turns error responses into an ApiError with the backend's message

const BASE_URL = import.meta.env.VITE_API_URL ?? ''
const TOKEN_KEY = 'stockpilot.auth'

export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export function getToken(): string | null {
  try {
    const raw = localStorage.getItem(TOKEN_KEY)
    return raw ? (JSON.parse(raw).token as string) : null
  } catch {
    return null
  }
}

// AuthContext registers a callback so a 401 anywhere logs the user out.
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    let fieldErrors: Record<string, string> = {}
    try {
      const body = await res.json()
      message = body.message ?? message
      fieldErrors = body.fieldErrors ?? {}
    } catch {
      // response had no JSON body
    }
    // Expired/invalid token on a protected call: log out. (Not for the login call itself.)
    if (res.status === 401 && !path.startsWith('/api/auth')) onUnauthorized()
    throw new ApiError(res.status, message, fieldErrors)
  }

  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const post = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'POST', body: JSON.stringify(body) })
export const put = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'PUT', body: JSON.stringify(body) })
export const patch = <T>(path: string, body: unknown) =>
  api<T>(path, { method: 'PATCH', body: JSON.stringify(body) })
export const del = (path: string) => api<void>(path, { method: 'DELETE' })

export { TOKEN_KEY }
