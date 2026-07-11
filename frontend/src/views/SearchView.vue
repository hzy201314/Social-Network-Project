<template>
  <div class="search-container">
    <!-- 背景层 -->
    <div class="bg-layer"></div>

    <div class="layout-wrapper">
      <!-- 1. 全局导航栏 -->
      <div class="left-sidebar">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="友趣 Logo" class="app-logo" />
        </div>
        <div class="nav-links">
          <button @click="goToFriends" class="nav-btn">👥</button>
          <button @click="goToProfile" class="nav-btn">👤</button>
          <button @click="goToCalendar" class="nav-btn">📅</button>
          <button @click="goToChat" class="nav-btn">💬</button>
          <button @click="$router.push('/home')" class="nav-btn">📋</button>
        </div>
      </div>

      <!-- 2. 右侧主内容区 (单栏居中布局) -->
      <div class="main-content">
        <div class="center-column">
          
          <!-- 顶部标题与搜索框 -->
          <div class="search-header">
            <h2 class="page-title">🔍 发现好友</h2>
            <div class="search-box-wrapper">
              <input
                v-model="keyword"
                type="text"
                placeholder="输入用户名、昵称或邮箱..."
                @input="onKeywordInput"
              />
            </div>

            <!-- 搜索方式选择 -->
            <div class="search-filter">
              <button
                v-for="option in searchOptions"
                :key="option.value"
                @click="selectedField = option.value; performSearch()"
                class="filter-btn"
                :class="{ active: selectedField === option.value }"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <!-- 搜索结果列表区 -->
          <div class="result-list-wrapper">
            <div v-if="loading" class="status-text">搜索中...</div>
            <div v-else-if="searchResults.length === 0 && searched" class="status-text">
              未找到匹配的用户
            </div>
            <div v-else v-for="user in searchResults" :key="user.id" class="user-result-item">
              <div class="user-info" @click="goToUserProfile(user.id)">
                <div class="avatar">
                  <img 
                    v-if="getUserAvatar(user.id, user.avatar, user.username)" 
                    :src="getUserAvatar(user.id, user.avatar, user.username)" 
                    alt="头像" 
                  />
                  <span v-else>{{ (user.nickname || user.username || '?').charAt(0) }}</span>
                </div>
                <div class="user-detail">
                  <div class="username">{{ getUserNickname(user.id, user.nickname, user.username) }}</div>
                  <div class="account">@{{ user.username }}</div>
                  <div class="user-email" v-if="user.email">{{ user.email }}</div>
                </div>
              </div>

              <button
                @click="sendRequest(user.id)"
                class="add-btn"
                :disabled="user.requestSent || user.friendStatus === '好友'"
              >
                {{ getButtonText(user) }}
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

const router = useRouter()

const keyword = ref('')
const searchResults = ref([])
const loading = ref(false)
const searched = ref(false)

// 搜索选项
const searchOptions = [
  { label: '全部', value: 'all' },
  { label: '用户名', value: 'username' },
  { label: '昵称', value: 'nickname' },
  { label: 'ID', value: 'id' },
  { label: '邮箱', value: 'email' }
]
const selectedField = ref('all')

// ===== 全局导航 =====
const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToCalendar = () => router.push('/calendar')
const goToChat = () => router.push('/chat')

// 防抖
let searchTimer = null
const onKeywordInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    performSearch()
  }, 300)
}

// 执行搜索
const performSearch = async () => {
  const kw = keyword.value.trim()
  if (!kw) {
    searchResults.value = []
    searched.value = false
    return
  }

  loading.value = true
  searched.value = true

  try {
    const res = await request.get('/api/users/search', {
      params: { keyword: kw, field: selectedField.value }
    })

    if (res.data.code === 0) {
      searchResults.value = (res.data.data || []).map(u => ({
        ...u,
        requestSent: false
      }))
    } else {
      alert(res.data.message || '搜索失败')
    }
  } catch (error) {
    console.error('搜索失败', error)
    alert('搜索失败，请重试')
  } finally {
    loading.value = false
  }
}

// 发送好友请求
const sendRequest = async (userId) => {
  try {
    const res = await request.post('/api/friends/requests', { friendId: userId })
    if (res.data.code === 0) {
      const user = searchResults.value.find(u => u.id === userId)
      if (user) {
        user.requestSent = true
        user.friendStatus = '待确认'
      }
      alert('好友请求已发送 ✅')
    } else {
      alert(res.data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送好友请求失败', error)
    alert('发送失败，请重试')
  }
}

// 按钮文字
const getButtonText = (user) => {
  if (user.requestSent) return '已发送'
  if (user.friendStatus === '好友') return '好友'
  if (user.friendStatus === '待确认') return '待确认'
  if (user.friendStatus === '已拒绝') return '已拒绝'
  return '添加好友'
}

const goToUserProfile = (userId) => {
  if (!userId) return
  const userStr = localStorage.getItem('user')
  let currentUserId = null
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUserId = user.id
    } catch (e) {}
  }
  if (userId === currentUserId) {
    router.push('/profile')
  } else {
    router.push(`/profile/${userId}`)
  }
}
</script>

<style>
body { margin: 0; padding: 0; }
</style>

<style scoped>
/* ============================================================ */
/* 1. 底层布局与背景 */
.search-container {
  width: 100vw; height: 100vh; position: relative; margin: 0; padding: 0; overflow: hidden;
}
.bg-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background-image: url('@/assets/profile-bg.png');
  background-size: cover; background-position: center;
  z-index: 1;
}
.layout-wrapper { width: 100%; height: 100%; position: absolute; top: 0; left: 0; z-index: 2; }

/* ============================================================ */
/* 2. 全局侧边栏 */
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
/* 3. 右侧主内容区 (单栏居中) */
.main-content {
  position: absolute; top: 0; left: 100px; right: 0; bottom: 0;
  display: flex; justify-content: center; padding: 30px 40px 20px 30px; box-sizing: border-box;
}
.center-column {
  width: 100%; max-width: 720px; /* 控制内容居中宽度 */
  height: 100%;
  display: flex; flex-direction: column;
}

.search-header {
  background: rgba(245, 255, 253, 0.35);
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  border-radius: 20px; padding: 24px 28px; margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
}
.page-title { margin: 0 0 16px 0; color: rgb(51, 69, 64); font-size: 20px; font-weight: 600; }

/* 搜索框 */
.search-box-wrapper {
  margin-bottom: 12px;
}
.search-box-wrapper input {
  width: 100%; padding: 12px 18px; border-radius: 40px; border: 1px solid rgba(51, 69, 64, 0.1);
  background: rgba(255, 255, 255, 0.4); font-size: 15px; color: rgb(51, 69, 64); outline: none; box-sizing: border-box; transition: 0.2s;
}
.search-box-wrapper input:focus { border-color: rgb(51, 69, 64); background: rgba(255, 255, 255, 0.6); }
.search-box-wrapper input::placeholder { color: rgba(51, 69, 64, 0.4); }

/* 筛选按钮 */
.search-filter { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-btn {
  padding: 6px 16px; border-radius: 20px; border: 1px solid rgba(51, 69, 64, 0.2);
  background: transparent; color: rgb(51, 69, 64); font-size: 13px; cursor: pointer; transition: 0.2s;
}
.filter-btn:hover { background: rgba(51, 69, 64, 0.05); }
.filter-btn.active { background: rgb(51, 69, 64); color: #fff; border-color: rgb(51, 69, 64); }

/* ============================================================ */
/* 4. 搜索结果列表 */
.result-list-wrapper {
  flex: 1; overflow-y: auto; padding-right: 8px;
  display: flex; flex-direction: column; gap: 12px;
}

.status-text { text-align: center; color: rgba(51, 69, 64, 0.6); padding: 40px 0; font-size: 14px; }

.user-result-item {
  background: rgba(245, 255, 253, 0.35);
  backdrop-filter: blur(6px); border-radius: 16px; padding: 16px 20px;
  display: flex; justify-content: space-between; align-items: center; gap: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2); position: relative; transition: 0.2s;
}
.user-result-item:hover { background: rgba(245, 255, 253, 0.55); }

/* 分割线 */
.user-result-item::after {
  content: ''; position: absolute; bottom: -6px; left: 0; width: 100%; height: 1px;
  background: linear-gradient(to right, rgba(51, 69, 64, 0), rgba(51, 69, 64, 0.15) 30%, rgba(51, 69, 64, 0.15) 70%, rgba(51, 69, 64, 0));
  pointer-events: none;
}
.user-result-item:last-child::after { display: none; }

.user-info { display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0; cursor: pointer; }
.avatar {
  width: 44px; height: 44px; border-radius: 50%; background: rgba(51, 69, 64, 0.15);
  color: rgb(51, 69, 64); display: flex; align-items: center; justify-content: center; font-weight: 600; overflow: hidden; flex-shrink: 0;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }

.user-detail { flex: 1; min-width: 0; }
.username { font-size: 16px; font-weight: 600; color: rgb(51, 69, 64); }
.account { font-size: 13px; color: rgba(51, 69, 64, 0.6); }
.user-email { font-size: 12px; color: rgba(51, 69, 64, 0.5); word-break: break-all; margin-top: 2px; }

/* 添加到右侧的按钮 */
.add-btn {
  padding: 6px 18px; border: none; border-radius: 20px; background: rgb(51, 69, 64);
  color: #fff; font-size: 13px; cursor: pointer; white-space: nowrap; transition: 0.2s; flex-shrink: 0;
}
.add-btn:hover:not(:disabled) { opacity: 0.8; transform: scale(1.02); }
.add-btn:disabled { background: rgba(51, 69, 64, 0.3); cursor: not-allowed; }
</style>