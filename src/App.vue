<script setup>
import PingAnTuoFu from './components/PingAnTuoFu.vue'
import { submitApplication } from './api'

// 提交申请：保存到后端数据库，由后端在生效日当天发送邮件通知
const apply = submitApplication
</script>

<template>
  <PingAnTuoFu :max-contacts="3" :submit-handler="apply" />
</template>
