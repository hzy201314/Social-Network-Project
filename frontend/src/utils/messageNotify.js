import request from '@/utils/request'
import { ref } from 'vue'

export const unreadCount = ref(0)          // 总未读数（通知 + 私聊）
export const unreadMessages = ref({})      // 每个好友的未读私聊数 { friendId: count }
export const notifications = ref([])       // 最新的未读通知列表

let previousNotificationIds = new Set()

// ===== 核心轮询函数 =====
export const fetchAllUnread = async () => {
  try {
    // 1. 获取通知列表
    const notiRes = await request.get('/api/notifications/unread')
    let newNotifications = []
    if (notiRes.data.code === 0) {
      newNotifications = notiRes.data.data || []
      notifications.value = newNotifications
    }

    // 2. 检查是否有新通知（弹窗）
    const currentIds = new Set(newNotifications.map(n => n.id))
    const hasNew = newNotifications.some(n => !previousNotificationIds.has(n.id))
    if (hasNew && newNotifications.length > 0) {
      const latest = newNotifications[0]
      showNotificationByType(latest)
    }
    previousNotificationIds = currentIds

    // 3. 获取未读通知数
    const countRes = await request.get('/api/notifications/unread/count')
    let notiCount = 0
    if (countRes.data.code === 0) {
      notiCount = countRes.data.data || 0
    }

    // 4. ✅ 获取私聊未读列表（接口是 /api/messages/unread）
    const msgRes = await request.get('/api/messages/unread')
    let msgCount = 0
    if (msgRes.data.code === 0) {
      const list = msgRes.data.data || []
      unreadMessages.value = {}
      list.forEach(item => {
        // 假设后端返回 { senderId: 3, count: 2 } 或 { friendId: 3, count: 2 }
        const friendId = item.friendId || item.senderId
        if (friendId) {
          unreadMessages.value[friendId] = item.count || 1
          msgCount += item.count || 1
        }
      })
    }

    // 5. 总未读数
    unreadCount.value = notiCount + msgCount
    console.log('📊 总未读数:', unreadCount.value, '通知:', notiCount, '私聊:', msgCount)

  } catch (error) {
    console.error('获取未读数据失败:', error)
  }
}

// ===== 弹窗显示 =====
const showNotificationByType = (notification) => {
  const typeMap = {
    'like': '❤️ 新的点赞',
    'comment': '💬 新的评论',
    'friend_request': '👤 新的好友请求'
  }
  const title = typeMap[notification.type] || '📢 新通知'
  const content = notification.content || '您有新消息'
  showToast(`${title}: ${content}`)

  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(title, { body: content, icon: '🔔' })
  }
}

// ===== Toast 弹窗 =====
const showToast = (message) => {
  const oldToast = document.querySelector('.custom-toast')
  if (oldToast) oldToast.remove()

  const toast = document.createElement('div')
  toast.className = 'custom-toast'
  toast.textContent = message
  document.body.appendChild(toast)

  setTimeout(() => toast.classList.add('show'), 10)
  setTimeout(() => {
    toast.classList.remove('show')
    setTimeout(() => toast.remove(), 300)
  }, 4000)
}

// ===== 获取某个好友的未读数 =====
export const getUnreadMessageCount = (friendId) => {
  return unreadMessages.value[friendId] || 0
}

// ===== 标记私聊为已读 =====
export const markMessageRead = async (friendId) => {
  try {
    // 1. 先获取与这位好友的所有未读消息
    const res = await request.get(`/api/messages/${friendId}`);
    if (res.data.code === 0) {
      const messages = res.data.data || [];
      // 2. 筛选出未读消息的 ID
      const unreadIds = messages
        .filter(msg => msg.senderId === friendId && msg.isRead === 0)
        .map(msg => msg.id);
      
      // 3. 如果有未读消息，调用批量标记已读接口
      if (unreadIds.length > 0) {
        await request.put('/api/messages/read', {
          messageIds: unreadIds
        });
        // 4. 更新本地未读数
        unreadMessages.value[friendId] = 0;
        await fetchAllUnread();
      }
    }
  } catch (error) {
    console.error('标记私聊已读失败:', error);
  }
};

// ===== 标记通知为已读 =====
export const markNotificationRead = async (notificationId) => {
  try {
    await request.put(`/api/notifications/${notificationId}/read`)
    await fetchAllUnread()
  } catch (error) {
    console.error('标记通知已读失败:', error)
  }
}

// ===== 标记所有通知为已读 =====
export const markAllNotificationsRead = async () => {
  try {
    await request.put('/api/notifications/read/all')
    await fetchAllUnread()
  } catch (error) {
    console.error('标记全部已读失败:', error)
  }
}

// ===== 初始化 =====
export const initNotification = async () => {
  await fetchAllUnread()
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
}