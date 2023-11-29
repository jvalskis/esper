import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      'balm-ui-plus': 'balm-ui/dist/balm-ui-plus.esm.js',
      'balm-ui-css': 'balm-ui/dist/balm-ui.css'
    }
  },
  build: {
    rollupOptions: {
      output: {
        // Define the main output bundle (e.g., for your application)
        entryFileNames: '[name]-[hash].js',
        // Define the directory where the main bundle should be output
        dir: 'dist',
        // Create a separate output bundle for your specific file
        manualChunks(id) {
          if (id.endsWith('src/config.ts')) {
            return 'config' // This will create a separate bundle for config-[hash].js
          }
        }
      },
    }
  }
})
