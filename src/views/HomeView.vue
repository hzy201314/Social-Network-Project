<template>
  <div class="home-container">
    <div class="layout-wrapper">
      
      <!-- 1. 上侧绿条 -->
      <div class="top-bar"></div>

      <!-- 2. 左侧绿条 (Logo + 功能按钮) -->
      <div class="left-sidebar">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="友趣 Logo" class="app-logo" />
        </div>
        
        <div class="nav-links">
          <button @click="goToFriends" class="nav-btn">👥</button>
          <button @click="goToProfile" class="nav-btn">👤</button>
          <button @click="goToChat" class="nav-btn">💬</button>
          <button @click="goToCalendar" class="nav-btn">📅</button>
          <button @click="toggleFeed" class="nav-btn" :class="{ active: isRecommend }">📋</button>
        </div>
      </div>

      <!-- 3. 独立悬浮的右上角功能栏 (发布、退出、搜索、头像) -->
      <div class="top-right-bar">
        <!-- ✅ 新增：发布按钮 -->
        <button class="action-btn publish-btn" @click="goToPublish">+ 发布</button>
        
        <!-- ✅ 新增：退出按钮 -->
        <button class="action-btn logout-btn" @click="handleLogout">退出</button>

        <div class="search-block" @click="goToSearch">
          <input type="text" placeholder="搜索..." @keyup.enter="goToSearch" />
        </div>
        
        <div class="top-avatar" @click="goToProfile">
          <img 
            v-if="currentUserAvatar" 
            :src="currentUserAvatar" 
            alt="头像"
          />
          <span v-else>{{ currentUserNickname.charAt(0).toUpperCase() }}</span>
        </div>
      </div>

      <!-- 4. 右侧主内容区：动态卡片 -->
      <div class="right-content">
        <div class="post-list-wrapper">
          <div v-if="loading" class="status-text">加载中...</div>
          <div v-else-if="posts.length === 0" class="status-text">暂无动态，快来发布第一条吧！</div>
          <div v-else v-for="post in posts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
            <div class="card-image">
              <img 
                v-if="postImages(post).length > 0" 
                :src="postImages(post)[0]" 
                :alt="post.title || post.content"
                loading="lazy"
              />
              <div v-else class="card-image-placeholder" :style="{ background: getRandomPastelColor() }">
                <span class="placeholder-title">{{ post.title || post.content }}</span>
              </div>
              <span v-if="postImages(post).length > 1" class="multi-image-badge">{{ postImages(post).length }}张</span>
            </div>
            
            <div class="card-content">
              <h3 class="card-title">{{ post.title || post.content }}</h3>
              <div v-if="post.tags && post.tags.length > 0" class="card-tags">
                <span v-for="tag in post.tags" :key="tag" class="card-tag">#{{ tag }}</span>
              </div>
              <div class="card-footer">
                <div class="card-user" @click.stop="goToUserProfile(post.userId)">
                  <span class="card-username">{{ post.nickname || post.username || '匿名用户' }}</span>
                </div>
                <div class="card-stats">
                  <span class="card-stat">❤️ {{ post.likesCount || 0 }}</span>
                  <span class="card-stat">💬 {{ post.commentCount || 0 }}</span>
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
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { unreadCount, fetchUnreadCount } from '@/utils/notification'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

const router = useRouter()
// 新增：跳转日历
const goToCalendar = () => router.push('/calendar')
const posts = ref([])
const loading = ref(true)
const isRecommend = ref(false)
let timer = null

const currentUserAvatar = computed(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return null
  try {
    const user = JSON.parse(userStr)
    return user.avatar || null
  } catch (e) { return null }
})

const currentUserNickname = computed(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return '我'
  try {
    const user = JSON.parse(userStr)
    return user.nickname || user.username || '我'
  } catch (e) { return '我' }
})

const getRandomPastelColor = () => {
  const colors = ['#f0f4ff', '#f5f0ff', '#f0fff4', '#fff5f0', '#fff0f5', '#f0faff', '#f5fff0', '#fff8f0', '#f0f5ff', '#f5f0f5', '#f0fff0', '#fff5f5']
  return colors[Math.floor(Math.random() * colors.length)]
}

const toggleFeed = () => {
  isRecommend.value = !isRecommend.value
  loadPosts()
}

const goToPostDetail = (postId) => {
  router.push(`/post/${postId}`)
}

const loadPosts = async () => {
  loading.value = true
  try {
    const url = isRecommend.value ? '/api/posts/recommend' : '/api/posts'
    const res = await request.get(url, {
      params: { page: 0, size: 20 }
    })
    if (res.data.code === 0) {
      const rawData = res.data.data || []
      posts.value = rawData.map(post => ({
        ...post,
        tags: Array.isArray(post.tags) ? post.tags : [],
        images: post.images ? (
          Array.isArray(post.images) ? post.images : 
          (typeof post.images === 'string' ? post.images.split(',').filter(u => u && u.trim()) : [])
        ) : [],
        comments: post.comments || []
      }))
    }
  } catch (error) {
    console.error('加载动态失败:', error)
  } finally {
    loading.value = false
  }
}

const postImages = (post) => {
  if (!post) return []
  if (!post.images) return []
  if (Array.isArray(post.images)) {
    return post.images
  }
  if (typeof post.images === 'string') {
    return post.images.split(',').filter(url => url && url.trim())
  }
  return []
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

const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToChat = () => router.push('/chat')
const goToSearch = () => router.push('/search')

// ✅ 添加这两个跳转函数（其实在原来的 script 里一直都有，这里确保万无一失）
const goToPublish = () => router.push('/publish')

const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }
}

onMounted(() => {
  loadPosts()
  fetchUnreadCount()
  timer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style>
body { margin: 0; padding: 0; background-color: #2c3e32; }
</style>

<style scoped>
/* ===== 1. 底层容器 ===== */
.home-container {
  width: 100vw;
  height: 100vh;
  background-image: url('@/assets/home-bg.png');
  background-size: cover;
  background-position: center;
  position: relative;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.layout-wrapper {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

/* ===== 2. 上侧绿条 ===== */
.top-bar {
  position: absolute;
  top: 0;
  left: 100px; 
  width: calc(100% - 100px);
  height: 60px;
  background: rgba(76, 95, 83, 1);
  z-index: 5;
}

/* ===== 3. 左侧绿条 ===== */
.left-sidebar {
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100vh;
  background: rgba(35, 63, 47, 0.64);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 18px;
}

.logo-wrapper {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px; /* Logo 和下方第一个按钮的距离 */
  flex-shrink: 0;
}

.app-logo {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 保证星星图片不变形，完全适应 50x50 的方块 */
  display: block;
}

.nav-links {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.nav-btn {
  background: rgba(237, 255, 249, 1);
  color: #2c3e32;
  border: none;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}
.nav-btn:hover { transform: translateY(-2px); background: #ffffff; }
.nav-btn.active { background: #e0e7ff; }

/* ===== 4. 右上角功能栏（发布、退出、搜索、头像） ===== */
.top-right-bar {
  position: absolute;
  top: 0;
  right: 0;
  height: 60px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding-right: 40px;
  box-sizing: border-box;
  z-index: 11;
  width: calc(100% - 100px);
}

/* ✅ 功能按钮样式 */
.action-btn {
  border: none;
  padding: 6px 16px;
  border-radius: 40px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  height: 32px;
  display: flex;
  align-items: center;
}

.publish-btn {
  background: rgba(237, 255, 249, 0.9);
  color: #2c3e32;
}
.publish-btn:hover {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.logout-btn {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.3);
}
.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ===== 搜索框 ===== */
.search-block {
  display: flex;
  align-items: center;
  width: 140px;
  height: 32px;
  background: rgb(213, 233, 221);
  border-radius: 40px;
  padding: 0 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.search-block:hover { background: rgb(230, 245, 235); }
.search-block input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  font-size: 13px;
  color: #2c3e32;
  cursor: pointer;
}
.search-block input::placeholder { color: #5d7468; }

/* ===== 头像 ===== */
.top-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgb(213, 233, 221);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #2c3e32;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}
.top-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ===== 5. 右侧卡片区 ===== */
.right-content {
  position: absolute;
  top: 60px;
  left: 100px;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  padding: 30px 40px;
  box-sizing: border-box;
}

.post-list-wrapper {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); 
  gap: 20px;
  padding-bottom: 20px;
}

.status-text {
  grid-column: 1 / -1;
  text-align: center;
  color: rgba(255, 255, 255, 0.9);
  padding: 60px 0;
}

.post-card {
  background: rgba(237, 255, 249, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 6px 16px rgba(35, 63, 47, 0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(35, 63, 47, 0.1);
}

.card-image {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 10px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.2);
}
.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #2c3e32;
  font-size: 14px;
}
.multi-image-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 12px;
  backdrop-filter: blur(4px);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.card-title {
  font-size: 15px !important;
  font-weight: 600 !important;
  color: #1d3326 !important;
  margin: 0 !important;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.card-tag {
  color: #2c3e32;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.6);
  padding: 1px 8px;
  border-radius: 10px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-username { font-size: 12px; color: #2c3e32; font-weight: 500; }
.card-stats { display: flex; gap: 10px; font-size: 12px; color: #385244; }
</style>