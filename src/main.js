import { createApp } from 'vue'
import App from './App.vue'
import './style.css'

// 标记运行环境，供样式区分浏览器预览 / 原生 App / 桌面端
const root = document.documentElement
if (window.Capacitor && window.Capacitor.isNativePlatform && window.Capacitor.isNativePlatform()) {
  root.classList.add('native-app')
  import('@capacitor/status-bar').then(({ StatusBar, Style }) => {
    // 浅色背景配深色文字
    StatusBar.setStyle({ style: Style.Light }).catch(() => {})
  })
}
// 仅打包后的桌面端（file://）标记，避免 Electron 内核浏览器误判
if (location.protocol === 'file:' && navigator.userAgent.includes('Electron')) {
  root.classList.add('desktop-app')
}

createApp(App).mount('#app')
