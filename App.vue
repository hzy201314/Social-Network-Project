<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// import { onMounted, onUnmounted } from 'vue'
// import { initNotification, fetchAllUnread } from '@/utils/messageNotify'
// import { useRouter } from 'vue-router'

// let pollingTimer = null
// const router = useRouter()

// onMounted(() => {
//   initNotification()
//   // 立即获取一次
//   fetchAllUnread()
//   // 每 3 秒轮询
//   pollingTimer = setInterval(fetchAllUnread, 3000)
// })

// onUnmounted(() => {
//   if (pollingTimer) {
//     clearInterval(pollingTimer)
//     pollingTimer = null
//   }
// })
</script>

<style>
.custom-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%) translateY(-100px);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 14px;
  z-index: 9999;
  max-width: 90%;
  text-align: center;
  transition: transform 0.3s ease, opacity 0.3s ease;
  opacity: 0;
  pointer-events: none;
}
.custom-toast.show {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
}
</style>