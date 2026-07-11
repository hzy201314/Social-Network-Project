<template>
  <div class="search-container">
    <!-- 顶部导航 -->
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>找 人</h1>
      <div style="width: 60px;"></div>
    </div>

    <!-- 搜索框 -->
    <div class="search-box">
      <input
        v-model="keyword"
        type="text"
        placeholder="输入关键词搜索..."
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

    <!-- 搜索结果 -->
    <div class="result-list">
      <div v-if="loading" class="status-text">搜索中...</div>
      <div v-else-if="searchResults.length === 0 && searched" class="status-text">
        未找到匹配的用户
      </div>
      <div v-else v-for="user in searchResults" :key="user.id" class="user-card">
        <div class="user-info" @click="goToUserProfile(user.id)">
          <!-- ✅ 头像：圆形带图片 -->
          <div class="avatar">
            <img 
              v-if="getUserAvatar(user.id, user.avatar, user.username)" 
              :src="getUserAvatar(user.id, user.avatar, user.username)" 
              alt="头像" 
            />
            <span v-else>{{ (user.nickname || user.username || '?').charAt(0) }}</span>
          </div>
          <div class="user-detail">
            <!-- ✅ 昵称 -->
            <div class="username">{{ getUserNickname(user.id, user.nickname, user.username) }}</div>
            <!-- ✅ 用户名 -->
            <div class="account">@{{ user.username }}</div>
            <div class="user-email" v-if="user.email">📧 {{ user.email }}</div>
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
      params: {
        keyword: kw,
        field: selectedField.value
      }
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
    const res = await request.post('/api/friends/requests', {
      friendId: userId
    })

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

const goBack = () => {
  router.push('/home')
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

<style scoped>
.search-container {
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

.header h1 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #667eea;
  padding: 4px 8px;
}

.search-box {
  margin: 16px 0 12px 0;
}

.search-box input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d0d7de;
  border-radius: 24px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.search-box input:focus {
  border-color: #667eea;
}

.search-filter {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filter-btn {
  padding: 6px 14px;
  border: 1px solid #d0d7de;
  border-radius: 16px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #555;
  transition: all 0.2s;
}

.filter-btn:hover {
  background: #f0f2f5;
}

.filter-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: #667eea;
}

.user-card {
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

/* ✅ 头像：圆形 + 图片裁剪 */
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
  flex-shrink: 0;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-detail .username {
  font-weight: 600;
  font-size: 15px;
}

.user-detail .account {
  font-size: 12px;
  color: #9aa6b5;
}

.user-detail .user-email {
  font-size: 12px;
  color: #9aa6b5;
  word-break: break-all;
}

.add-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 16px;
  background: #667eea;
  color: white;
  cursor: pointer;
  font-size: 13px;
  flex-shrink: 0;
}

.add-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.status-text {
  text-align: center;
  color: #9aa6b5;
  padding: 40px 0;
  font-size: 15px;
}
</style>