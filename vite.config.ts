import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import pkg from './package.json'
import { randomUUID } from 'node:crypto'

const buildId = randomUUID()

export default defineConfig({
  define: {
    __BUILD_ID__: JSON.stringify(buildId),
    __APP_VERSION__: JSON.stringify(pkg.version),
    __BUILD_DATE__: JSON.stringify(new Date().toLocaleDateString('ru-RU')),
  },
  plugins: [react(), tailwindcss(), {
    name: 'panel-build-id',
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'panel-build.json', source: JSON.stringify({ buildId }) })
    },
  }],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    },
  },
})
