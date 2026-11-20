import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 相对路径：兼顾 Web 部署、Capacitor 与 Electron 的 file:// 加载
  base: './',
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
