<script setup>
import PingAnTuoFu from './components/PingAnTuoFu.vue'

// 提交成功后的出口：这里替换为实际接口调用
function handleSubmit(payload) {
  console.log('[apply]', payload)
}
</script>

<template>
  <PingAnTuoFu :max-contacts="3" @submit="handleSubmit" />
</template>
