// 后端地址：Web 端留空走同源 /api（开发时由 Vite 代理）；
// 打包 App 时通过 VITE_API_BASE 指定，例如 VITE_API_BASE=https://api.example.com
const API_BASE = (import.meta.env.VITE_API_BASE || '').trim().replace(/\/+$/, '')

function isNativePlatform() {
  return !!(window.Capacitor && window.Capacitor.isNativePlatform && window.Capacitor.isNativePlatform())
}

function needsRemoteBase() {
  return isNativePlatform() || navigator.userAgent.includes('Electron')
}

export async function submitApplication(payload) {
  if (needsRemoteBase() && !API_BASE) {
    throw new Error('未配置后端地址（VITE_API_BASE），请重新打包')
  }

  const res = await fetch(`${API_BASE}/api/applications`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      effectiveDate: payload.date,
      note: payload.note,
      contacts: payload.contacts
    })
  })

  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(data.message || `提交失败（${res.status}）`)
  }
  return data
}
