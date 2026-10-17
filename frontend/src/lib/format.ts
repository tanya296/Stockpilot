const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })

export const money = (n: number) => inr.format(n)

// The backend sends UTC timestamps; the browser shows them in the user's local time.
export const dateTime = (iso: string) =>
  new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
