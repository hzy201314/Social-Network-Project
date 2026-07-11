<template>
  <div class="notifications-container">
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>消息通知</h1>
      <button @click="markAllRead" class="read-btn" v-if="notifications.length > 0">
        全部已读
      </button>
    </div>

    <div class="notification-list">
      <div v-if="loading" class="status-text">加载中...</div>
      <div v-else-if="notifications.length === 0" class="status-text">暂无通知</div>
      <div v-else v-for="item in notifications" :key="item.id" class="notification-item" @click="handleNotificationClick(item)">
        <div class="notification-icon">{{ getIcon(item.type) }}</div>
        <div class="notification-content">
          <div class="notification-text">{{ item.content || '您有新消息' }}</div>
          <div class="notification-time">{{ formatTime(item.createdAt) }}</div>
        </div>
        <div v-if="item.isRead === 0" class="unread-dot"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { formatTime } from '@/utils/dateUtils'
import { notifications, fetchAllUnread, markNotificationRead, markAllNotificationsRead } from '@/utils/messageNotify'

const router = useRouter()
const loading = ref(true)

const getIcon = (type) => {
  const map = { 'like': '❤️', 'comment': '💬', 'friend_request': '👤' }
  return map[type] || '📢'
}

const handleNotificationClick = async (item) => {
  if (item.isRead === 0) {
    await markNotificationRead(item.id)
  }
  // 跳转
  if (item.type === 'like' || item.type === 'comment') {
    router.push(`/post/${item.targetId}`)
  } else if (item.type === 'friend_request') {
    router.push('/friends')
  } else {
    router.push('/home')
  }
}

const markAllRead = async () => {
  await markAllNotificationsRead()
  await fetchAllUnread()
}

const goBack = () => router.push('/home')

onMounted(async () => {
  await fetchAllUnread()
  loading.value = false
})
</script>

<style scoped>
.notifications-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 16px 80px 16px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 12px 0;
  border-bottom: 1px solid #e8ecf1;
}
.header h1 { font-size: 18px; font-weight: 600; margin: 0; }
.back-btn { background: none; border: none; font-size: 16px; cursor: pointer; color: #667eea; padding: 4px 8px; }
.read-btn { background: none; border: none; font-size: 14px; cursor: pointer; color: #52c41a; padding: 4px 8px; }
.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: background 0.2s;
}
.notification-item:hover { background: #f0f2f5; }
.notification-icon { font-size: 24px; flex-shrink: 0; }
.notification-content { flex: 1; min-width: 0; }
.notification-text { font-size: 14px; color: #333; word-break: break-word; }
.notification-time { font-size: 12px; color: #9aa6b5; margin-top: 4px; }
.unread-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #667eea;
  flex-shrink: 0;
}
.status-text { text-align: center; color: #9aa6b5; padding: 40px 0; font-size: 15px; }
</style>