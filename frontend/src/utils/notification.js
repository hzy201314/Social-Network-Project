import request from '@/utils/request'
import { ref } from 'vue'

// 未读通知数量
export const unreadCount = ref(0)

// 获取未读通知数
export const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/api/notifications/unread/count')
    if (res.data.code === 0) {
      unreadCount.value = res.data.data || 0
    }
  } catch (error) {
    console.error('获取未读通知数失败', error)
  }
}

// 获取通知列表
export const fetchNotifications = async () => {
  try {
    const res = await request.get('/api/notifications')
    if (res.data.code === 0) {
      return res.data.data || []
    }
  } catch (error) {
    console.error('获取通知列表失败', error)
    return []
  }
}

// 标记所有通知为已读
export const markAllAsRead = async () => {
  try {
    const res = await request.post('/api/notifications/mark-read')
    if (res.data.code === 0) {
      unreadCount.value = 0
    }
  } catch (error) {
    console.error('标记已读失败', error)
  }
}