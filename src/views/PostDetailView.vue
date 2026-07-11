<template>
  <div class="post-detail-container">
    <!-- 背景层 -->
    <div class="bg-layer"></div>

    <div class="layout-wrapper">
      <!-- 1. 全局左侧导航栏 -->
      <div class="left-sidebar">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="友趣 Logo" class="app-logo" />
        </div>
        <div class="nav-links">
          <button @click="goToFriends" class="nav-btn">👥</button>
          <button @click="goToProfile" class="nav-btn">👤</button>
          <button @click="goToCalendar" class="nav-btn">📅</button>
          <button @click="goToChat" class="nav-btn">💬</button>
          <button @click="$router.push('/home')" class="nav-btn active">📋</button>
        </div>
      </div>

      <!-- 2. 右侧核心内容区 -->
      <div class="main-content">
        <div class="detail-header">
          <button @click="goBack" class="back-btn">← 返回</button>
          <span class="detail-title">动态详情</span>
        </div>

        <!-- 详情卡片：分为左右两栏 -->
        <div v-if="loading" class="loading-text">加载中...</div>
        <div v-else-if="!post" class="empty-text">动态不存在或已被删除</div>
        
        <div v-else class="detail-wrapper">
          
          <!-- ===== 左半部分：完整的动态内容 (标题、图片、正文) ===== -->
          <div class="detail-left">
            
            <!-- 标题 -->
            <h1 class="post-title">{{ post.title || post.content }}</h1>
            
            <!-- 标签 -->
            <div v-if="post.tags && post.tags.length > 0" class="post-tags">
              <span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
            </div>

            <!-- 图片/媒体区 -->
            <div class="media-container">
              <!-- 有图片时显示轮播 -->
              <div v-if="postImages.length > 0" class="image-carousel">
                <div class="carousel-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
                  <div v-for="(url, index) in postImages" :key="index" class="carousel-slide">
                    <img :src="url" :alt="'图片' + (index + 1)" loading="lazy" />
                  </div>
                </div>
              
                <!-- 左右箭头 -->
                <button v-if="postImages.length > 1" @click="prevImage" class="carousel-btn prev">‹</button>
                <button v-if="postImages.length > 1" @click="nextImage" class="carousel-btn next">›</button>
              
                <!-- 指示器 -->
                <div v-if="postImages.length > 1" class="carousel-dots">
                  <span v-for="i in postImages.length" :key="i" class="dot" :class="{ active: i - 1 === currentIndex }"></span>
                </div>
              </div>
            </div>

            <!-- 正文 -->
            <div class="post-body">{{ post.content }}</div>

          </div>

          <!-- ===== 右半部分：点赞与评论互动区 ===== -->
          <div class="detail-right">
            <!-- 作者信息 -->
            <div class="author-info" @click="goToUserProfile(post.userId)">
              <div class="author-avatar">
                <img v-if="post.avatar" :src="post.avatar" alt="" />
                <span v-else>{{ (post.nickname || post.username || 'U').charAt(0) }}</span>
              </div>
              <div class="author-name">{{ post.nickname || post.username || '匿名用户' }}</div>
            </div>

            <!-- 操作栏：点赞与删除 -->
            <div class="detail-actions">
              <button @click="handleLike" class="action-btn like-btn">
                <span>{{ post.liked ? '❤️' : '🤍' }}</span>
                <span>{{ post.likesCount || 0 }}</span>
              </button>
              <button @click="toggleComment" class="action-btn comment-btn">
                <span>💬</span>
                <span>{{ post.commentCount || 0 }}</span>
              </button>
              <button v-if="isPostOwner" @click="confirmDeletePost" class="delete-btn">🗑️ 删除</button>
            </div>

            <!-- ✅ 评论列表 -->
            <div class="detail-comments">
              <div v-if="post.comments && post.comments.length > 0">
                <CommentTree
                  v-for="comment in post.comments"
                  :key="comment.id"
                  :comment="comment"
                  :level="0"
                  :show-delete="true" 
                  @reply="(commentId, username) => replyToComment(commentId, username)"
                  @delete="(commentId) => deleteComment(commentId)"
                />
              </div>
              <div v-else class="no-comments">暂无评论，抢个沙发吧！</div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import CommentTree from '@/components/CommentTree.vue'

const router = useRouter()
const route = useRoute()
const post = ref(null)
const loading = ref(true)
const currentIndex = ref(0)

// ===== 全局导航 =====
const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToCalendar = () => router.push('/calendar')
const goToChat = () => router.push('/chat')

// ===== 获取当前用户 =====
const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) { try { return JSON.parse(userStr) } catch (e) { return {} } }
  return {}
}
const currentUser = getCurrentUser()
const isPostOwner = computed(() => post.value && currentUser.id === post.value.userId)

// ===== 图片处理 =====
const postImages = computed(() => {
  if (!post.value) return []
  if (!post.value.images) return []
  if (Array.isArray(post.value.images)) return post.value.images.filter(url => url && url.trim())
  if (typeof post.value.images === 'string') return post.value.images.split(',').filter(url => url && url.trim())
  return []
})

// ===== 加载动态详情 =====
const loadPost = async () => {
  const postId = route.params.postId
  if (!postId) { loading.value = false; return }
  loading.value = true
  try {
    const res = await request.get('/api/posts', { params: { page: 0, size: 50 } })
    if (res.data.code === 0) {
      const foundPost = res.data.data.find(p => p.id === parseInt(postId))
      if (foundPost) {
        try {
          const commentsRes = await request.get(`/api/posts/${foundPost.id}/comments`)
          const allComments = commentsRes.data.data || []
          const addReadyState = (comments) => {
            return comments.map(comment => ({
              ...comment, isReady: true, isPending: false,
              replies: comment.replies ? addReadyState(comment.replies) : []
            }))
          }
          foundPost.comments = addReadyState(allComments)
        } catch (e) { foundPost.comments = [] }
        foundPost.tags = Array.isArray(foundPost.tags) ? foundPost.tags : []
        foundPost.images = Array.isArray(foundPost.images) ? foundPost.images : []
        post.value = foundPost
      } else { post.value = null }
    }
  } catch (error) { console.error('加载动态失败:', error); post.value = null } 
  finally { loading.value = false }
}

// ===== 图片轮播 =====
const nextImage = () => {
  if (currentIndex.value < postImages.value.length - 1) currentIndex.value++
  else currentIndex.value = 0
}
const prevImage = () => {
  if (currentIndex.value > 0) currentIndex.value--
  else currentIndex.value = postImages.value.length - 1
}

// ===== 点赞 =====
const handleLike = async () => {
  if (!post.value) return
  try {
    const res = await request.post(`/api/posts/${post.value.id}/like`)
    if (res.data.code === 0) { post.value.liked = !post.value.liked; post.value.likesCount = res.data.data }
  } catch (error) { console.error('点赞失败:', error) }
}

// ===== 评论 =====
const toggleComment = () => {
  const content = prompt('请输入评论内容：')
  if (!content || !content.trim()) return
  const userStr = localStorage.getItem('user')
  let currentUserInfo = { username: '我', nickname: '我', avatar: null }
  if (userStr) { try { const user = JSON.parse(userStr); currentUserInfo = { username: user.username || '我', nickname: user.nickname || user.username || '我', avatar: user.avatar || null } } catch (e) {} }

  const tempComment = { id: Date.now(), username: currentUserInfo.username, nickname: currentUserInfo.nickname, avatar: currentUserInfo.avatar, content: content.trim(), parentId: null, createdAt: new Date().toISOString(), replies: [], isReady: false, isPending: true }
  if (!post.value.comments) post.value.comments = []
  post.value.comments.push(tempComment)
  post.value.commentCount = (post.value.commentCount || 0) + 1

  request.post(`/api/posts/${post.value.id}/comments`, { content: content.trim() })
    .then(res => {
      if (res.data.code === 0) {
        const realComment = res.data.data
        const idx = post.value.comments.findIndex(c => c.id === tempComment.id)
        if (idx !== -1) {
          post.value.comments[idx] = { ...realComment, id: tempComment.id, username: currentUserInfo.username, nickname: currentUserInfo.nickname, avatar: currentUserInfo.avatar, replies: [], isReady: true, isPending: false }
        }
        alert('评论成功！')
      } else {
        post.value.comments = post.value.comments.filter(c => c.id !== tempComment.id)
        post.value.commentCount = (post.value.commentCount || 0) - 1
        alert(res.data.message || '评论失败')
      }
    })
    .catch(() => { post.value.comments = post.value.comments.filter(c => c.id !== tempComment.id); post.value.commentCount = (post.value.commentCount || 0) - 1; alert('评论失败，请重试') })
}

// ===== 回复评论 =====
const replyToComment = (parentCommentId, parentUsername) => {
  const content = prompt(`回复 @${parentUsername || '用户'}：`)
  if (!content || !content.trim()) return
  const findParent = (list) => { for (const c of list) { if (c.id === parentCommentId) return c; if (c.replies && c.replies.length > 0) { const found = findParent(c.replies); if (found) return found } } return null }
  const parent = findParent(post.value.comments)
  if (!parent || !parent.isReady) return alert('父评论不存在或正在加载')

  const userStr = localStorage.getItem('user')
  let currentUserInfo = { username: '我', nickname: '我', avatar: null }
  if (userStr) { try { const user = JSON.parse(userStr); currentUserInfo = { username: user.username || '我', nickname: user.nickname || user.username || '我', avatar: user.avatar || null } } catch (e) {} }

  const tempComment = { id: Date.now(), username: currentUserInfo.username, nickname: currentUserInfo.nickname, avatar: currentUserInfo.avatar, content: content.trim(), parentId: parentCommentId, createdAt: new Date().toISOString(), replies: [], isReady: false }
  if (!parent.replies) parent.replies = []
  parent.replies.push(tempComment)
  post.value.commentCount = (post.value.commentCount || 0) + 1

  const removeTempComment = (list) => { for (let i = 0; i < list.length; i++) { if (list[i].id === tempComment.id) { list.splice(i, 1); return true } if (list[i].replies && removeTempComment(list[i].replies)) return true } return false }

  request.post(`/api/posts/${post.value.id}/comments`, { content: content.trim(), parentId: parentCommentId })
    .then(res => {
      if (res.data.code === 0) {
        const realComment = res.data.data
        const replaceInList = (list) => { for (let i = 0; i < list.length; i++) { if (list[i].id === tempComment.id) { list[i] = { ...realComment, id: tempComment.id, username: currentUserInfo.username, nickname: currentUserInfo.nickname, avatar: currentUserInfo.avatar, replies: [], isReady: true }; return true } if (list[i].replies && replaceInList(list[i].replies)) return true } return false }
        replaceInList(post.value.comments)
        alert('回复成功！')
      } else {
        removeTempComment(post.value.comments)
        post.value.commentCount = (post.value.commentCount || 0) - 1
        alert(res.data.message || '回复失败')
      }
    })
    .catch(() => { removeTempComment(post.value.comments); post.value.commentCount = (post.value.commentCount || 0) - 1; alert('回复失败，请重试') })
}

// ===== 删除评论 =====
const deleteComment = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  if (!post.value) return
  // 直接在本地抹除
  const removeComment = (list) => {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === commentId) {
        list.splice(i, 1)
        return true
      }
      if (list[i].replies && removeComment(list[i].replies)) return true
    }
    return false
  }
  if (removeComment(post.value.comments)) {
    post.value.commentCount = (post.value.commentCount || 0) - 1
  }
  try { await request.delete(`/api/posts/comments/${commentId}`); alert('评论已删除') } catch (error) { alert('删除失败，请重试') }
}

// ===== 删除动态 =====
const confirmDeletePost = () => {
  if (!confirm('确定要删除这条动态吗？')) return
  request.delete(`/api/posts/${post.value.id}`).then(() => { alert('动态已删除'); router.push('/home') }).catch(() => alert('删除失败'))
}

// ===== 跳转 =====
const goBack = () => router.push('/home')
const goToUserProfile = (userId) => {
  if (!userId) return
  const userStr = localStorage.getItem('user')
  let currentUserId = null
  if (userStr) { try { const user = JSON.parse(userStr); currentUserId = user.id } catch (e) {} }
  router.push(userId === currentUserId ? '/profile' : `/profile/${userId}`)
}

onMounted(loadPost)
</script>

<style>
body { margin: 0; padding: 0; }
</style>

<style scoped>
/* ============================================================ */
/* 1. 底层容器 */
.post-detail-container {
  width: 100vw; height: 100vh; position: relative; margin: 0; padding: 0; overflow: hidden;
}
.bg-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background-image: url('@/assets/chat-bg.png');
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
/* 2. 右侧主区域布局 */
.main-content {
  position: absolute; top: 0; left: 100px; right: 0; bottom: 0;
  display: flex; flex-direction: column; padding: 30px 40px 20px 30px; box-sizing: border-box;
}
.detail-header {
  display: flex; align-items: center; gap: 16px; margin-bottom: 20px; flex-shrink: 0;
}
.back-btn { background: rgba(245, 255, 253, 0.35); border: none; border-radius: 40px; padding: 8px 16px; color: rgb(51, 69, 64); font-size: 14px; cursor: pointer; transition: 0.2s; display: flex; align-items: center; gap: 6px; }
.back-btn:hover { background: rgba(245, 255, 253, 0.6); }
.detail-title { color: rgb(51, 69, 64); font-size: 18px; font-weight: 600; }

.loading-text, .empty-text { text-align: center; color: rgba(255, 255, 255, 0.8); padding: 60px 0; font-size: 16px; }

/* ============================================================ */
/* 3. 详情毛玻璃大卡片 (左右分栏) */
.detail-wrapper {
  flex: 1; display: flex; gap: 20px; overflow: hidden;
  background: rgba(245, 255, 253, 0.35); backdrop-filter: blur(12px);
  border-radius: 20px; border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 20px; box-sizing: border-box;
}

/* ===== 左侧：完整的动态图文区 ===== */
.detail-left {
  flex: 1.4; height: 100%; overflow-y: auto; padding-right: 20px; box-sizing: border-box;
}
.post-title { font-size: 24px; font-weight: 700; color: rgb(51, 69, 64); margin: 0 0 8px 0; }
.post-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px; }
.post-tags .tag { color: rgb(51, 69, 64); background: rgba(255,255,255,0.3); padding: 2px 10px; border-radius: 12px; font-size: 13px; }

.media-container {
  width: 100%; aspect-ratio: 16/9; border-radius: 16px; overflow: hidden; background: rgba(0,0,0,0.05);
  position: relative; margin-bottom: 16px;
}
.image-carousel { width: 100%; height: 100%; position: relative; overflow: hidden; }
.carousel-track { display: flex; height: 100%; transition: transform 0.3s ease; }
.carousel-slide { min-width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f7fa; }
.carousel-slide img { width: 100%; height: 100%; object-fit: contain; }

.carousel-btn {
  position: absolute; top: 50%; transform: translateY(-50%);
  background: rgba(51, 69, 64, 0.5); color: white; border: none;
  border-radius: 50%; width: 36px; height: 36px; font-size: 20px; cursor: pointer; z-index: 5;
}
.carousel-btn:hover { background: rgba(51, 69, 64, 0.8); }
.prev { left: 12px; } .next { right: 12px; }

.carousel-dots {
  position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 8px; z-index: 5;
}
.dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,0.4); transition: background 0.3s; }
.dot.active { background: #fff; }

.post-body { font-size: 16px; line-height: 1.8; color: rgb(51, 69, 64); white-space: pre-wrap; }

/* ===== 右侧：互动评论区 ===== */
.detail-right {
  flex: 1; height: 100%; padding: 0 0 0 20px; box-sizing: border-box;
  display: flex; flex-direction: column; overflow: hidden; border-left: 1px solid rgba(51, 69, 64, 0.1);
}
.author-info { display: flex; align-items: center; gap: 12px; cursor: pointer; margin-bottom: 12px; }
.author-avatar { width: 40px; height: 40px; border-radius: 50%; background: rgba(51, 69, 64, 0.15); color: rgb(51, 69, 64); display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600; overflow: hidden; flex-shrink: 0; }
.author-avatar img { width: 100%; height: 100%; object-fit: cover; }
.author-name { font-size: 16px; font-weight: 600; color: rgb(51, 69, 64); }

.detail-actions {
  display: flex; gap: 20px; padding: 12px 0; border-top: 1px solid rgba(51, 69, 64, 0.1);
  border-bottom: 1px solid rgba(51, 69, 64, 0.1); flex-shrink: 0;
}
.action-btn { background: none; border: none; cursor: pointer; font-size: 16px; color: rgb(51, 69, 64); padding: 4px 8px; display: flex; align-items: center; gap: 6px; transition: 0.2s; }
.action-btn:hover { transform: scale(1.05); }
.delete-btn { background: none; border: none; cursor: pointer; font-size: 14px; color: #d0314e; padding: 4px 8px; margin-left: auto; }

.detail-comments { flex: 1; overflow-y: auto; margin-top: 12px; padding-right: 4px; }
.no-comments { color: rgba(51, 69, 64, 0.5); font-size: 14px; padding: 20px 0; text-align: center; }
</style>