<template>
  <div class="friends-container">
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>我的好友</h1>
      <div style="width: 60px;"></div>
    </div>

    <div class="tabs">
      <button @click="activeTab = 'friends'" :class="['tab', { active: activeTab === 'friends' }]">
        好友 ({{ friends.length }})
      </button>
      <button @click="activeTab = 'requests'" :class="['tab', { active: activeTab === 'requests' }]">
        请求 ({{ pendingRequests.length }})
      </button>
    </div>

    <!-- 好友列表 -->
    <div v-if="activeTab === 'friends'" class="list">
      <div v-if="loading" class="status-text">加载中...</div>
      <div v-else-if="friends.length === 0" class="status-text">还没有好友，去添加吧！</div>
      <div v-else v-for="friend in friends" :key="friend.id" class="friend-card">
        <div class="user-info" @click="goChat(friend.id)">
          <div class="avatar-wrapper">
            <div class="avatar">{{ friend.nickname?.charAt(0) || friend.username?.charAt(0) || '?' }}</div>
            <!-- ✅ 每个好友头像右上角的红点 -->
            <span v-if="getUnreadCount(friend.id) > 0" class="unread-badge">{{ getUnreadCount(friend.id) }}</span>
          </div>
          <div class="user-detail">
            <div class="username">{{ friend.nickname || friend.username }}</div>
            <div class="account">@{{ friend.username }}</div>
          </div>
        </div>
        <div class="friend-actions">
          <button @click="confirmDelete(friend.id, friend.nickname || friend.username)" class="delete-btn">删</button>
        </div>
      </div>
    </div>

    <!-- 好友请求列表 -->
    <div v-if="activeTab === 'requests'" class="list">
      <div v-if="loading" class="status-text">加载中...</div>
      <div v-else-if="pendingRequests.length === 0" class="status-text">暂无好友请求</div>
      <div v-else v-for="req in pendingRequests" :key="req.id" class="request-card">
        <div class="user-info">
          <div class="avatar">
            <img 
              v-if="getUserAvatar(friend.id, friend.avatar, friend.username)" 
              :src="getUserAvatar(friend.id, friend.avatar, friend.username)" 
              alt="头像" 
            />
            <span v-else>{{ (friend.nickname || friend.username || '?').charAt(0) }}</span>
          </div>
          <div class="user-detail">
            <div class="username">{{ getUserNickname(friend.id, friend.nickname, friend.username) }}</div>
            <div class="account">@{{ friend.username }}</div>
          </div>
          <div class="user-detail">
            <div class="username">{{ req.nickname || req.username || '未知用户' }}</div>
            <div class="account">@{{ req.username || '' }}</div>
          </div>
        </div>
        <div class="request-actions">
          <button @click="handleRequest(req.id, 'accept')" class="accept-btn">同意</button>
          <button @click="handleRequest(req.id, 'reject')" class="reject-btn">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { getUnreadMessageCount, fetchAllUnread, markMessageRead } from '@/utils/messageNotify'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

const router = useRouter()
const activeTab = ref('friends')
const loading = ref(true)
const friends = ref([])
const pendingRequests = ref([])
let timer = null

// ===== 获取好友列表 =====
const loadFriends = async () => {
  try {
    const res = await request.get('/api/friends')
    if (res.data.code === 0) {
      friends.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载好友失败', error)
  }
}

// ===== 加载好友请求列表 =====
const loadRequests = async () => {
  try {
    const res = await request.get('/api/friends/requests')
    if (res.data.code === 0) {
      pendingRequests.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载请求失败', error)
  }
}

// ===== 获取某个好友的未读私聊数 =====
const getUnreadCount = (friendId) => {
  return getUnreadMessageCount(friendId)
}

// ===== 处理好友请求 =====
const handleRequest = async (requestId, action) => {
  const status = action === 'accept' ? 1 : 2
  try {
    const res = await request.put(`/api/friends/requests/${requestId}`, {
      status: status
    })
    if (res.data.code === 0) {
      if (action === 'accept') {
        const req = pendingRequests.value.find(r => r.id === requestId)
        if (req) {
          const alreadyExists = friends.value.some(f => f.id === req.userId)
          if (!alreadyExists) {
            friends.value.push({
              id: req.userId,
              username: req.username,
              nickname: req.nickname,
              avatar: req.avatar,
              status: 1
            })
          }
        }
        alert('已同意好友请求 ✅')
      } else {
        alert('已拒绝 ❌')
      }
      await loadRequests()
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (error) {
    console.error('处理请求失败', error)
    alert('操作失败，请重试')
  }
}

// ===== 删除好友 =====
const confirmDelete = (friendId, friendName) => {
  if (confirm(`确定要删除好友 "${friendName}" 吗？`)) {
    deleteFriend(friendId)
  }
}

const deleteFriend = async (friendId) => {
  try {
    const res = await request.delete(`/api/friends/${friendId}`)
    if (res.data.code === 0) {
      friends.value = friends.value.filter(f => f.id !== friendId)
      alert('已删除好友')
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除好友失败', error)
    alert('删除失败，请重试')
  }
}

// ===== 进入聊天 =====
const goChat = async (friendId) => {
  await markMessageRead(friendId)
  router.push(`/chat/${friendId}`)
}

// ===== 返回 =====
const goBack = () => router.push('/home')

// ===== 生命周期 =====
onMounted(async () => {
  loading.value = true
  await loadFriends()
  await loadRequests()
  await fetchAllUnread()
  loading.value = false

  // 每 5 秒刷新未读数据
  timer = setInterval(fetchAllUnread, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.friends-container {
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

.tabs {
  display: flex;
  gap: 0;
  margin: 16px 0;
  background: #f0f2f5;
  border-radius: 12px;
  padding: 4px;
}
.tab {
  flex: 1;
  padding: 8px 0;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  background: transparent;
  color: #666;
  transition: all 0.3s;
}
.tab.active {
  background: white;
  color: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.friend-card,
.request-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff4d4f;
  color: white;
  border-radius: 50%;
  min-width: 20px;
  height: 20px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 600;
  border: 2px solid white;
}

.user-detail {
  flex: 1;
  min-width: 0;
}
.user-detail .username { font-weight: 600; font-size: 15px; }
.user-detail .account { font-size: 12px; color: #9aa6b5; }

.friend-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.chat-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 16px;
  background: #f0f2f5;
  color: #333;
  cursor: pointer;
  font-size: 13px;
}
.chat-btn:hover { background: #e4e7ed; }

.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 16px;
  background: #ff4d4f;
  color: white;
  cursor: pointer;
  font-size: 13px;
}
.delete-btn:hover { background: #e04345; }

.request-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.accept-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 16px;
  background: #52c41a;
  color: white;
  cursor: pointer;
  font-size: 13px;
}
.reject-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 16px;
  background: #ff4d4f;
  color: white;
  cursor: pointer;
  font-size: 13px;
}

.status-text {
  text-align: center;
  color: #9aa6b5;
  padding: 40px 0;
  font-size: 15px;
}

.post-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
</style>