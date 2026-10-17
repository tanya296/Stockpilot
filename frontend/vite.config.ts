import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // In development, the browser calls /api/... on the Vite server, which forwards
    // to Spring Boot. Same origin for the browser, so no CORS issues locally.
    proxy: {
      '/api': 'http://localhost:8090',
    },
  },
})
