<template>
  <div class="home-container">
    <div class="content-wrapper">
      <!-- 顶部导航 -->
      <div class="header">
        <div class="header-left">
          <h1>🌐 社交圈</h1>
          <div class="notification-bell" @click="goToNotifications">
            🔔
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
          </div>
        </div>
        <div class="header-right">
          <button @click="goToPublish" class="btn-publish">+ 发布</button>
          <button @click="handleLogout" class="btn-logout">退出</button>
        </div>
      </div>

      <!-- 导航链接 -->
      <div class="nav-links">
        <button @click="goToFriends" class="nav-btn">👥 好友</button>
        <button @click="goToSearch" class="nav-btn">🔍 找人</button>
        <button @click="goToProfile" class="nav-btn">👤 我的</button>
        <button @click="goToChat" class="nav-btn">💬 聊天</button>
        <button @click="toggleFeed" class="nav-btn" :class="{ active: isRecommend }">
          {{ isRecommend ? '🌟 推荐' : '📋 最新' }}
        </button>
      </div>

      <!-- 卡片式动态列表 -->
      <div class="post-list">
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
            <!-- ✅ 标签：只有作者加了才显示 -->
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
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { formatTime } from '@/utils/dateUtils'
import CommentTree from '@/components/CommentTree.vue'
import { unreadCount, fetchUnreadCount } from '@/utils/notification'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

const router = useRouter()
const posts = ref([])
const loading = ref(true)
const isRecommend = ref(false)
let timer = null

// ===== 随机浅色背景 =====
const getRandomPastelColor = () => {
  const colors = ['#f0f4ff', '#f5f0ff', '#f0fff4', '#fff5f0', '#fff0f5', '#f0faff', '#f5fff0', '#fff8f0', '#f0f5ff', '#f5f0f5', '#f0fff0', '#fff5f5']
  return colors[Math.floor(Math.random() * colors.length)]
}

// ===== 切换推荐/最新 =====
const toggleFeed = () => {
  isRecommend.value = !isRecommend.value
  loadPosts()
}

// ===== 跳转到动态详情 =====
const goToPostDetail = (postId) => {
  router.push(`/post/${postId}`)
}

// ===== 加载动态 =====
const loadPosts = async () => {
  loading.value = true
  try {
    const url = isRecommend.value ? '/api/posts/recommend' : '/api/posts'
    const res = await request.get(url, {
      params: { page: 0, size: 20 }
    })
    if (res.data.code === 0) {
      const rawData = res.data.data || []
      console.log('📦 后端返回数据示例:', rawData[0]) // ✅ 调试日志
      
      posts.value = rawData.map(post => ({
        ...post,
        // ✅ 确保 tags 是数组
        tags: Array.isArray(post.tags) ? post.tags : [],
        // ✅ 确保 images 是数组
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

// ===== 图片处理 =====
const postImages = (post) => {
  if (!post) return []
  if (!post.images) return []
  // ✅ 后端返回的就是数组，直接返回
  if (Array.isArray(post.images)) {
    return post.images
  }
  // 如果是字符串，按逗号分割
  if (typeof post.images === 'string') {
    return post.images.split(',').filter(url => url && url.trim())
  }
  return []
}

// ===== 判断是否是当前用户的动态 =====
const isPostOwner = (post) => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return false
  try {
    const user = JSON.parse(userStr)
    return user.id === post.userId
  } catch (e) { return false }
}

// ===== 点赞 =====
const handleLike = async (postId) => {
  try {
    const res = await request.post(`/api/posts/${postId}/like`)
    if (res.data.code === 0) {
      const post = posts.value.find(p => p.id === postId)
      if (post) {
        post.liked = !post.liked
        post.likesCount = res.data.data
      }
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (error) {
    console.error('点赞失败', error)
    alert('点赞失败，请重试')
  }
}

// ===== ✅ 评论功能 =====
const toggleComment = (postId) => {
  const content = prompt('请输入评论内容：')
  if (!content || !content.trim()) return

  const post = posts.value.find(p => p.id === postId)
  if (!post) return

  const userStr = localStorage.getItem('user')
  let currentUser = { username: '我', nickname: '我', avatar: null }
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser = {
        username: user.username || '我',
        nickname: user.nickname || user.username || '我',
        avatar: user.avatar || null
      }
    } catch (e) {}
  }

  const tempComment = {
    id: Date.now(),
    username: currentUser.username,
    nickname: currentUser.nickname,
    avatar: currentUser.avatar,
    content: content.trim(),
    parentId: null,
    createdAt: new Date().toISOString(),
    replies: [],
    isReady: false,
    isPending: true
  }

  if (!post.comments) post.comments = []
  post.comments.push(tempComment)
  post.commentCount = (post.commentCount || 0) + 1

  request.post(`/api/posts/${postId}/comments`, { content: content.trim() })
    .then(res => {
      if (res.data.code === 0) {
        const realComment = res.data.data
        const idx = post.comments.findIndex(c => c.id === tempComment.id)
        if (idx !== -1) {
          post.comments[idx] = {
            ...realComment,
            id: tempComment.id,
            username: currentUser.username,
            nickname: currentUser.nickname,
            avatar: currentUser.avatar,
            replies: [],
            isReady: true,
            isPending: false
          }
        }
        alert('评论成功！')
      } else {
        post.comments = post.comments.filter(c => c.id !== tempComment.id)
        post.commentCount = (post.commentCount || 0) - 1
        alert(res.data.message || '评论失败')
      }
    })
    .catch(() => {
      post.comments = post.comments.filter(c => c.id !== tempComment.id)
      post.commentCount = (post.commentCount || 0) - 1
      alert('评论失败，请重试')
    })
}

// ===== ✅ 回复评论 =====
const replyToComment = (postId, commentId, username) => {
  const content = prompt(`回复 @${username || '用户'}：`)
  if (!content || !content.trim()) return

  const post = posts.value.find(p => p.id === postId)
  if (!post) {
    alert('找不到动态')
    return
  }

  const findParent = (list) => {
    for (const c of list) {
      if (c.id === commentId) return c
      if (c.replies && c.replies.length > 0) {
        const found = findParent(c.replies)
        if (found) return found
      }
    }
    return null
  }

  const parent = findParent(post.comments)
  if (!parent) {
    alert('父评论不存在')
    return
  }
  if (!parent.isReady) {
    alert('父评论正在加载中')
    return
  }

  const userStr = localStorage.getItem('user')
  let currentUser = { username: '我', nickname: '我', avatar: null }
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUser = {
        username: user.username || '我',
        nickname: user.nickname || user.username || '我',
        avatar: user.avatar || null
      }
    } catch (e) {}
  }

  const tempComment = {
    id: Date.now(),
    username: currentUser.username,
    nickname: currentUser.nickname,
    avatar: currentUser.avatar,
    content: content.trim(),
    parentId: commentId,
    createdAt: new Date().toISOString(),
    replies: [],
    isReady: false
  }

  if (!parent.replies) parent.replies = []
  parent.replies.push(tempComment)
  post.commentCount = (post.commentCount || 0) + 1

  const removeTempComment = (list) => {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === tempComment.id) {
        list.splice(i, 1); return true
      }
      if (list[i].replies && removeTempComment(list[i].replies)) return true
    }
    return false
  }

  request.post(`/api/posts/${postId}/comments`, {
    content: content.trim(),
    parentId: commentId
  })
  .then(res => {
    if (res.data.code === 0) {
      const realComment = res.data.data
      const replaceInList = (list) => {
        for (let i = 0; i < list.length; i++) {
          if (list[i].id === tempComment.id) {
            list[i] = { ...realComment, id: tempComment.id, username: currentUser.username, nickname: currentUser.nickname, avatar: currentUser.avatar, replies: [], isReady: true }
            return true
          }
          if (list[i].replies && replaceInList(list[i].replies)) return true
        }
        return false
      }
      replaceInList(post.comments)
      alert('回复成功！')
    } else {
      removeTempComment(post.comments)
      post.commentCount = (post.commentCount || 0) - 1
      alert(res.data.message || '回复失败')
    }
  })
  .catch(() => {
    removeTempComment(post.comments)
    post.commentCount = (post.commentCount || 0) - 1
    alert('回复失败，请重试')
  })
}

// ===== ✅ 删除评论 =====
const deleteComment = async (postId, commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  const post = posts.value.find(p => p.id === postId)
  if (!post) return

  const markDeleted = (list) => {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === commentId) {
        list[i].isDeleted = true
        return true
      }
      if (list[i].replies && markDeleted(list[i].replies)) return true
    }
    return false
  }

  markDeleted(post.comments)
  post.commentCount = (post.commentCount || 0) - 1

  try {
    await request.delete(`/api/posts/comments/${commentId}`)
    alert('评论已删除')
  } catch (error) {
    console.error('删除评论失败:', error)
    alert('删除失败，请重试')
  }
}

// ===== 其他函数 =====
const goToNotifications = () => router.push('/notifications')
const goToPublish = () => router.push('/publish')
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
const goToFriends = () => router.push('/friends')
const goToSearch = () => router.push('/search')
const goToProfile = () => router.push('/profile')
const goToChat = () => router.push('/chat')
const confirmDeletePost = (postId) => {
  if (confirm('确定要删除这条动态吗？')) {
    request.delete(`/api/posts/${postId}`).then(() => {
      posts.value = posts.value.filter(p => p.id !== postId)
      alert('动态已删除')
    }).catch(() => alert('删除失败'))
  }
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

// ===== 生命周期 =====
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
body { margin: 0; padding: 0; background-color: #f5f7fa; }
</style>

<style scoped>
/* 样式和之前一致，保持不变 */
.home-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  justify-content: center;
  padding: 0 20px 60px 20px;
  box-sizing: border-box;
}
.content-wrapper {
  width: 100%;
  max-width: 600px;
}
.header, .nav-links, .post-list {
  width: 100%;
  box-sizing: border-box;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 8px 0;
  border-bottom: 1px solid #e8ecf1;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-left h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.notification-bell {
  position: relative;
  font-size: 22px;
  cursor: pointer;
  transition: transform 0.2s;
}
.notification-bell:hover { transform: scale(1.1); }
.badge {
  position: absolute;
  top: -6px;
  right: -8px;
  background: #ff4d4f;
  color: white;
  border-radius: 50%;
  min-width: 18px;
  height: 18px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}
.btn-publish {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  transition: transform 0.1s ease;
}
.btn-publish:hover { transform: scale(1.02); }
.btn-logout {
  background: #ffffff;
  border: 1px solid #d0d7de;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  transition: all 0.1s ease;
}
.btn-logout:hover { background: #f0f0f0; border-color: #b0b8c0; }

.nav-links {
  display: flex;
  gap: 8px;
  padding: 10px 0 16px 0;
  border-bottom: 1px solid #e8ecf1;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.nav-btn {
  background: none;
  border: none;
  font-size: 14px;
  color: #555;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.nav-btn:hover { background: #f0f2f5; }
.nav-btn.active {
  background: #667eea;
  color: white;
}

.post-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.status-text {
  grid-column: 1 / -1;
  text-align: center;
  color: #9aa6b5;
  padding: 60px 0;
  font-size: 15px;
}

.post-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
  overflow: hidden;
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
  padding: 20px;
  box-sizing: border-box;
  text-align: center;
}
.placeholder-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.multi-image-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
}

.card-content {
  padding: 12px 14px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: #1f2a3a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}
.card-tag {
  color: #667eea;
  font-size: 12px;
  background: #f0f2f5;
  padding: 1px 8px;
  border-radius: 10px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}
.card-user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.card-username {
  font-size: 13px;
  color: #666;
}
.card-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #999;
}
.card-stat {
  display: flex;
  align-items: center;
  gap: 3px;
}

@media (max-width: 480px) {
  .post-list {
    grid-template-columns: 1fr;
  }
}
</style>