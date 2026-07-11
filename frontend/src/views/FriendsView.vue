<template>
  <div class="friends-container">
    <!-- 背景层 -->
    <div class="bg-layer"></div>

    <div class="layout-wrapper">
      <!-- 1. 全局导航栏 -->
      <div class="left-sidebar">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="友趣 Logo" class="app-logo" />
        </div>
        <div class="nav-links">
          <button @click="goToFriends" class="nav-btn active">👥</button>
          <button @click="goToProfile" class="nav-btn">👤</button>
          <button @click="goToCalendar" class="nav-btn">📅</button>
          <button @click="goToChat" class="nav-btn">💬</button>
          <button @click="$router.push('/home')" class="nav-btn">📋</button>
        </div>
      </div>

      <!-- 2. 右侧主内容区 -->
      <div class="main-content">
        <div class="top-actions">
          <h2 class="page-title">👥 我的好友</h2>
          <button @click="$router.push('/search')" class="add-btn">+ 添加好友</button>
        </div>

        <div class="content-grid">
          
          <!-- ===== 左半部分：好友列表 (半透明大框) ===== -->
          <div class="friend-list-card">
            <div class="list-header">
              <span class="list-title">好友列表</span>
              <span class="list-count">{{ friends.length }} 人</span>
            </div>

            <div class="friend-items">
              <div v-if="loading" class="status-text">加载中...</div>
              <div v-else-if="friends.length === 0" class="status-text">还没有好友，去添加吧！</div>
              
              <div v-else v-for="friend in friends" :key="friend.id" class="friend-row-item">
                <div class="friend-info" @click="goChat(friend.id)">
                  <div class="friend-avatar">
                    <img v-if="friend.avatar" :src="friend.avatar" alt="头像" />
                    <span v-else>{{ (friend.nickname || friend.username || '?').charAt(0) }}</span>
                  </div>
                  <div class="friend-detail">
                    <div class="friend-name">{{ friend.nickname || friend.username }}</div>
                    <div class="friend-account">@{{ friend.username }}</div>
                  </div>
                </div>
                
                <div class="friend-actions">
                  <button @click="goChat(friend.id)" class="chat-btn">💬</button>
                  <button @click="confirmDelete(friend.id, friend.nickname || friend.username)" class="del-btn">✕</button>
                </div>
              </div>
            </div>
          </div>

          <!-- ===== 右半部分：好友请求列表 (半透明框) ===== -->
          <div class="request-card">
            <div class="list-header">
              <span class="list-title">🤝 好友请求</span>
              <span class="list-count">{{ pendingRequests.length }} 条</span>
            </div>

            <div class="friend-items">
              <div v-if="loading" class="status-text">加载中...</div>
              <div v-else-if="pendingRequests.length === 0" class="status-text">暂无好友请求</div>
              
              <div v-else v-for="req in pendingRequests" :key="req.id" class="request-row-item">
                <div class="friend-info">
                  <div class="friend-avatar">
                    <img v-if="req.avatar" :src="req.avatar" alt="头像" />
                    <span v-else>{{ (req.nickname || req.username || '?').charAt(0) }}</span>
                  </div>
                  <div class="friend-detail">
                    <div class="friend-name">{{ req.nickname || req.username }}</div>
                    <div class="friend-account">@{{ req.username || '' }}</div>
                  </div>
                </div>
                
                <div class="request-actions">
                  <button @click="handleRequest(req.id, 'accept')" class="accept-btn">同意</button>
                  <button @click="handleRequest(req.id, 'reject')" class="reject-btn">拒绝</button>
                </div>
              </div>
            </div>
          </div>

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

const router = useRouter()
const loading = ref(true)
const friends = ref([])
const pendingRequests = ref([])
let timer = null

// ===== 全局导航 =====
const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToCalendar = () => router.push('/calendar')
const goToChat = () => router.push('/chat')

// ===== 获取好友列表 =====
const loadFriends = async () => {
  try {
    const res = await request.get('/api/friends')
    if (res.data.code === 0) {
      friends.value = res.data.data || []
    }
  } catch (error) { console.error('加载好友失败', error) }
}

// ===== 加载好友请求列表 =====
const loadRequests = async () => {
  try {
    const res = await request.get('/api/friends/requests')
    if (res.data.code === 0) {
      pendingRequests.value = res.data.data || []
    }
  } catch (error) { console.error('加载请求失败', error) }
}

// ===== 处理好友请求 =====
const handleRequest = async (requestId, action) => {
  const status = action === 'accept' ? 1 : 2
  try {
    const res = await request.put(`/api/friends/requests/${requestId}`, { status })
    if (res.data.code === 0) {
      if (action === 'accept') {
        const req = pendingRequests.value.find(r => r.id === requestId)
        if (req && !friends.value.some(f => f.id === req.userId)) {
          friends.value.push({ id: req.userId, username: req.username, nickname: req.nickname, avatar: req.avatar, status: 1 })
        }
        alert('已同意好友请求 ✅')
      } else {
        alert('已拒绝 ❌')
      }
      await loadRequests()
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (error) { alert('操作失败，请重试') }
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
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (error) { alert('删除失败，请重试') }
}

// ===== 进入聊天 =====
const goChat = async (friendId) => {
  await markMessageRead(friendId)
  router.push(`/chat`)
}

// ===== 生命周期 =====
onMounted(async () => {
  loading.value = true
  await loadFriends()
  await loadRequests()
  await fetchAllUnread()
  loading.value = false
  timer = setInterval(fetchAllUnread, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style>
body { margin: 0; padding: 0; }
</style>

<style scoped>
/* ============================================================ */
/* 1. 背景与容器 */
.friends-container {
  width: 100vw; height: 100vh; position: relative; margin: 0; padding: 0; overflow: hidden;
}
.bg-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background-image: url('@/assets/profile-bg.png');
  background-size: cover; background-position: center;
  z-index: 1;
}
.layout-wrapper { width: 100%; height: 100%; position: absolute; top: 0; left: 0; z-index: 2; }

/* ===== 导航栏 ===== */
.left-sidebar {
  position: absolute; top: 0; left: 0; width: 100px; height: 100vh;
  background: rgba(35, 63, 47, 0.64); z-index: 10;
  display: flex; flex-direction: column; align-items: center; padding-top: 18px;
}
.logo-wrapper {
  width: 50px; height: 50px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 40px; flex-shrink: 0;
}
.app-logo {
  width: 100%; height: 100%;
  object-fit: contain; display: block;
}
.nav-links { display: flex; flex-direction: column; align-items: center; gap: 24px; }
.nav-btn {
  background: rgba(237, 255, 249, 0.45); color: #2c3e32; border: none;
  width: 46px; height: 46px; border-radius: 14px; font-size: 22px;
  display: flex; align-items: center; justify-content: center; padding: 0; cursor: pointer; transition: all 0.2s;
}
.nav-btn:hover { transform: translateY(-2px); }
.nav-btn.active { background: rgba(237, 255, 249, 0.8); }

/* ============================================================ */
/* 2. 右侧主布局 (左右分栏) */
.main-content {
  position: absolute; top: 0; left: 100px; right: 0; bottom: 0;
  display: flex; flex-direction: column; padding: 30px 40px 20px 30px; box-sizing: border-box;
}
.top-actions {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-shrink: 0;
}
.page-title { margin: 0; color: rgb(51, 69, 64); font-size: 20px; font-weight: 600; }
.add-btn {
  background: rgba(51, 69, 64, 0.8); border: none; border-radius: 20px; padding: 8px 18px; color: #fff; font-size: 14px; cursor: pointer; transition: 0.2s;
}
.add-btn:hover { background: rgb(51, 69, 64); }

.content-grid { flex: 1; display: flex; gap: 24px; overflow: hidden; }

/* ============================================================ */
/* 3. 左右毛玻璃框 (透明35) */
.friend-list-card, .request-card {
  flex: 1; height: 100%;
  background: rgba(245, 255, 253, 0.35);
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  border-radius: 16px; padding: 24px; box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex; flex-direction: column;
}

.list-header {
  display: flex; justify-content: space-between; align-items: center; padding-bottom: 16px; border-bottom: 1px solid rgba(51, 69, 64, 0.1); flex-shrink: 0;
}
.list-title { font-size: 16px; font-weight: 600; color: rgb(51, 69, 64); }
.list-count { font-size: 13px; color: rgba(51, 69, 64, 0.6); }

.friend-items { flex: 1; overflow-y: auto; padding-top: 16px; display: flex; flex-direction: column; gap: 2px; }

.status-text { text-align: center; color: rgba(51, 69, 64, 0.6); padding: 40px 0; font-size: 14px; }

/* ===== 好友单条 ===== */
.friend-row-item {
  display: flex; justify-content: space-between; align-items: center; padding: 12px 8px; position: relative; transition: 0.2s; border-radius: 8px;
}
.friend-row-item:hover { background: rgba(255, 255, 255, 0.3); }
.friend-row-item::after {
  content: ''; position: absolute; bottom: 0; left: 0; width: 100%; height: 1px;
  background: linear-gradient(to right, rgba(51, 69, 64, 0), rgba(51, 69, 64, 0.15) 30%, rgba(51, 69, 64, 0.15) 70%, rgba(51, 69, 64, 0));
  pointer-events: none;
}
.friend-row-item:last-child::after { display: none; }

.friend-info { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; cursor: pointer; }
.friend-avatar {
  width: 40px; height: 40px; border-radius: 50%; background: rgba(51, 69, 64, 0.15);
  color: rgb(51, 69, 64); display: flex; align-items: center; justify-content: center; font-weight: 600; overflow: hidden; flex-shrink: 0;
}
.friend-avatar img { width: 100%; height: 100%; object-fit: cover; }
.friend-detail { flex: 1; min-width: 0; }
.friend-name { font-size: 15px; font-weight: 600; color: rgb(51, 69, 64); }
.friend-account { font-size: 12px; color: rgba(51, 69, 64, 0.6); }

.friend-actions { display: flex; gap: 6px; flex-shrink: 0; }
.chat-btn, .del-btn { background: none; border: none; font-size: 16px; cursor: pointer; padding: 4px; transition: 0.2s; }
.chat-btn:hover { transform: scale(1.1); }
.del-btn:hover { color: #d0314e; transform: scale(1.1); }

/* ===== 请求单条 ===== */
.request-row-item {
  display: flex; justify-content: space-between; align-items: center; padding: 12px 8px; border-bottom: 1px solid rgba(51, 69, 64, 0.1);
}
.request-row-item:last-child { border-bottom: none; }

.request-actions { display: flex; gap: 8px; flex-shrink: 0; }
.accept-btn { padding: 4px 14px; border: none; border-radius: 16px; background: rgb(51, 69, 64); color: #fff; cursor: pointer; font-size: 13px; transition: 0.2s; }
.accept-btn:hover { opacity: 0.8; }
.reject-btn { padding: 4px 14px; border: 1px solid rgba(51, 69, 64, 0.3); border-radius: 16px; background: transparent; color: rgb(51, 69, 64); cursor: pointer; font-size: 13px; transition: 0.2s; }
.reject-btn:hover { background: rgba(208, 49, 78, 0.05); color: #d0314e; border-color: #d0314e; }
</style>