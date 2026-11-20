<script setup>
import PingAnTuoFu from './components/PingAnTuoFu.vue'

// 提交申请：保存到后端 H2，并由后端发送邮件通知
async function apply(payload) {
  const res = await fetch('/api/applications', {
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
</script>

<template>
  <PingAnTuoFu :max-contacts="3" :submit-handler="apply" />
</template>
