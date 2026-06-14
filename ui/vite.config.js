import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  base: command === 'build' ? '/ai/match/' : './',
  plugins: [
    vue(),
   vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    allowedHosts: [
      'gussie-conjugated-imprudently.ngrok-free.dev',
      'localhost',
      '127.0.0.1'
    ],
    proxy: {
      '/ai/match/api': {
        target: 'http://localhost:8080',
        secure: false,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai\/match\/api/, ''),
      }
    }
  }
}))
