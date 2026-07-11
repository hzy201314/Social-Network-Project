<template>
  <div class="post-detail-container">
    <!-- 返回按钮 -->
    <div class="detail-header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <span class="detail-title">动态详情</span>
      <div style="width: 50px;"></div>
    </div>

    <div v-if="loading" class="loading-text">加载中...</div>
    <div v-else-if="!post" class="empty-text">动态不存在或已被删除</div>
    <div v-else class="detail-content">
      <!-- 左半：图片轮播 -->
      <div class="detail-left">
        <div class="image-carousel">
          <!-- 有图片时显示轮播 -->
          <div v-if="postImages.length > 0" class="carousel-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
            <div v-for="(url, index) in postImages" :key="index" class="carousel-slide">
              <img :src="url" :alt="'图片' + (index + 1)" loading="lazy" />
            </div>
          </div>
        
          <!-- ✅ 没有图片时显示标题占位图（和封面样式一致） -->
          <div v-else class="no-image-placeholder" :style="{ background: getRandomPastelColor() }">
            <span class="placeholder-title">{{ post.title || post.content }}</span>
          </div>
        
          <!-- 左右箭头（只有多张图片时显示） -->
          <button v-if="postImages.length > 1" @click="prevImage" class="carousel-btn prev">‹</button>
          <button v-if="postImages.length > 1" @click="nextImage" class="carousel-btn next">›</button>
        
          <!-- 指示器（只有多张图片时显示） -->
          <div v-if="postImages.length > 1" class="carousel-dots">
            <span v-for="i in postImages.length" :key="i" class="dot" :class="{ active: i - 1 === currentIndex }"></span>
          </div>
        </div>
      </div>

      <!-- 右半：内容 + 互动 -->
      <div class="detail-right">
        <div class="author-info" @click="goToUserProfile(post.userId)">
          <div class="author-avatar">
            <img v-if="post.avatar" :src="post.avatar" alt="" />
            <span v-else>{{ (post.nickname || post.username || 'U').charAt(0) }}</span>
          </div>
          <div class="author-name">{{ post.nickname || post.username || '匿名用户' }}</div>
        </div>

        <h2 class="detail-title-text">{{ post.title || post.content }}</h2>

        <!-- ✅ 标签 -->
        <div v-if="post.tags && post.tags.length > 0" class="detail-tags">
          <span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
        </div>

        <div class="detail-body" ref="bodyRef">
          <p>{{ post.content }}</p>
        </div>

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
              :show-delete="isPostOwner"
              @reply="(commentId, username) => replyToComment(commentId, username)"
              @delete="(commentId) => deleteComment(commentId)"
            />
          </div>
          <div v-else class="no-comments">暂无评论</div>
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

// ===== 获取当前用户 =====
const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch (e) { return {} }
  }
  return {}
}
const currentUser = getCurrentUser()
const isPostOwner = computed(() => {
  return post.value && currentUser.id === post.value.userId
})

// ===== 随机浅色背景 =====
const getRandomPastelColor = () => {
  const colors = ['#f0f4ff', '#f5f0ff', '#f0fff4', '#fff5f0', '#fff0f5', '#f0faff', '#f5fff0', '#fff8f0', '#f0f5ff', '#f5f0f5', '#f0fff0', '#fff5f5']
  return colors[Math.floor(Math.random() * colors.length)]
}

// ===== ✅ 图片处理 =====
const postImages = computed(() => {
  if (!post.value) return []
  if (!post.value.images) return []
  if (Array.isArray(post.value.images)) {
    return post.value.images.filter(url => url && url.trim())
  }
  if (typeof post.value.images === 'string') {
    return post.value.images.split(',').filter(url => url && url.trim())
  }
  return []
})

// ===== 加载动态详情 =====
const loadPost = async () => {
  const postId = route.params.postId
  if (!postId) {
    loading.value = false
    return
  }

  loading.value = true
  try {
    const res = await request.get('/api/posts', { params: { page: 0, size: 50 } })
    if (res.data.code === 0) {
      const foundPost = res.data.data.find(p => p.id === parseInt(postId))
      if (foundPost) {
        // 加载评论
        try {
          const commentsRes = await request.get(`/api/posts/${foundPost.id}/comments`)
          const allComments = commentsRes.data.data || []
          const addReadyState = (comments) => {
            return comments.map(comment => ({
              ...comment,
              isReady: true,
              isPending: false,
              replies: comment.replies ? addReadyState(comment.replies) : []
            }))
          }
          foundPost.comments = addReadyState(allComments)
        } catch (e) {
          foundPost.comments = []
        }
        // ✅ 确保 tags 是数组
        foundPost.tags = Array.isArray(foundPost.tags) ? foundPost.tags : []
        // ✅ 确保 images 是数组
        foundPost.images = Array.isArray(foundPost.images) ? foundPost.images : []
        post.value = foundPost
        console.log('📦 PostDetail 加载成功:', post.value)
      } else {
        console.warn('未找到动态:', postId)
        post.value = null
      }
    }
  } catch (error) {
    console.error('加载动态失败:', error)
    post.value = null
  } finally {
    loading.value = false
  }
}

// ===== 图片轮播 =====
const nextImage = () => {
  if (currentIndex.value < postImages.value.length - 1) {
    currentIndex.value++
  } else {
    currentIndex.value = 0
  }
}
const prevImage = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  } else {
    currentIndex.value = postImages.value.length - 1
  }
}

// ===== 点赞 =====
const handleLike = async () => {
  if (!post.value) return
  try {
    const res = await request.post(`/api/posts/${post.value.id}/like`)
    if (res.data.code === 0) {
      post.value.liked = !post.value.liked
      post.value.likesCount = res.data.data
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// ===== ✅ 评论 =====
const toggleComment = () => {
  const content = prompt('请输入评论内容：')
  if (!content || !content.trim()) return

  const userStr = localStorage.getItem('user')
  let currentUserInfo = { username: '我', nickname: '我', avatar: null }
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUserInfo = {
        username: user.username || '我',
        nickname: user.nickname || user.username || '我',
        avatar: user.avatar || null
      }
    } catch (e) {}
  }

  const tempComment = {
    id: Date.now(),
    username: currentUserInfo.username,
    nickname: currentUserInfo.nickname,
    avatar: currentUserInfo.avatar,
    content: content.trim(),
    parentId: null,
    createdAt: new Date().toISOString(),
    replies: [],
    isReady: false,
    isPending: true
  }

  if (!post.value.comments) post.value.comments = []
  post.value.comments.push(tempComment)
  post.value.commentCount = (post.value.commentCount || 0) + 1

  request.post(`/api/posts/${post.value.id}/comments`, { content: content.trim() })
    .then(res => {
      if (res.data.code === 0) {
        const realComment = res.data.data
        const idx = post.value.comments.findIndex(c => c.id === tempComment.id)
        if (idx !== -1) {
          post.value.comments[idx] = {
            ...realComment,
            id: tempComment.id,
            username: currentUserInfo.username,
            nickname: currentUserInfo.nickname,
            avatar: currentUserInfo.avatar,
            replies: [],
            isReady: true,
            isPending: false
          }
        }
        alert('评论成功！')
      } else {
        post.value.comments = post.value.comments.filter(c => c.id !== tempComment.id)
        post.value.commentCount = (post.value.commentCount || 0) - 1
        alert(res.data.message || '评论失败')
      }
    })
    .catch(() => {
      post.value.comments = post.value.comments.filter(c => c.id !== tempComment.id)
      post.value.commentCount = (post.value.commentCount || 0) - 1
      alert('评论失败，请重试')
    })
}

// ===== ✅ 回复评论 =====
const replyToComment = (parentCommentId, parentUsername) => {
  const content = prompt(`回复 @${parentUsername || '用户'}：`)
  if (!content || !content.trim()) return

  const findParent = (list) => {
    for (const c of list) {
      if (c.id === parentCommentId) return c
      if (c.replies && c.replies.length > 0) {
        const found = findParent(c.replies)
        if (found) return found
      }
    }
    return null
  }

  const parent = findParent(post.value.comments)
  if (!parent) {
    alert('父评论不存在')
    return
  }
  if (!parent.isReady) {
    alert('父评论正在加载中')
    return
  }

  const userStr = localStorage.getItem('user')
  let currentUserInfo = { username: '我', nickname: '我', avatar: null }
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUserInfo = {
        username: user.username || '我',
        nickname: user.nickname || user.username || '我',
        avatar: user.avatar || null
      }
    } catch (e) {}
  }

  const tempComment = {
    id: Date.now(),
    username: currentUserInfo.username,
    nickname: currentUserInfo.nickname,
    avatar: currentUserInfo.avatar,
    content: content.trim(),
    parentId: parentCommentId,
    createdAt: new Date().toISOString(),
    replies: [],
    isReady: false
  }

  if (!parent.replies) parent.replies = []
  parent.replies.push(tempComment)
  post.value.commentCount = (post.value.commentCount || 0) + 1

  const removeTempComment = (list) => {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === tempComment.id) {
        list.splice(i, 1); return true
      }
      if (list[i].replies && removeTempComment(list[i].replies)) return true
    }
    return false
  }

  request.post(`/api/posts/${post.value.id}/comments`, {
    content: content.trim(),
    parentId: parentCommentId
  })
  .then(res => {
    if (res.data.code === 0) {
      const realComment = res.data.data
      const replaceInList = (list) => {
        for (let i = 0; i < list.length; i++) {
          if (list[i].id === tempComment.id) {
            list[i] = { ...realComment, id: tempComment.id, username: currentUserInfo.username, nickname: currentUserInfo.nickname, avatar: currentUserInfo.avatar, replies: [], isReady: true }
            return true
          }
          if (list[i].replies && replaceInList(list[i].replies)) return true
        }
        return false
      }
      replaceInList(post.value.comments)
      alert('回复成功！')
    } else {
      removeTempComment(post.value.comments)
      post.value.commentCount = (post.value.commentCount || 0) - 1
      alert(res.data.message || '回复失败')
    }
  })
  .catch(() => {
    removeTempComment(post.value.comments)
    post.value.commentCount = (post.value.commentCount || 0) - 1
    alert('回复失败，请重试')
  })
}

// ===== ✅ 删除评论 =====
const deleteComment = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  if (!post.value) return

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

  markDeleted(post.value.comments)
  post.value.commentCount = (post.value.commentCount || 0) - 1

  try {
    await request.delete(`/api/posts/comments/${commentId}`)
    alert('评论已删除')
  } catch (error) {
    console.error('删除评论失败:', error)
    alert('删除失败，请重试')
  }
}

// ===== 删除动态 =====
const confirmDeletePost = () => {
  if (!confirm('确定要删除这条动态吗？')) return
  request.delete(`/api/posts/${post.value.id}`)
    .then(() => {
      alert('动态已删除')
      router.push('/home')
    })
    .catch(() => alert('删除失败'))
}

// ===== 跳转 =====
const goBack = () => router.push('/home')
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

onMounted(loadPost)
</script>

<style scoped>
/* 样式保持不变 */
.post-detail-container {
  max-width: 600px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #e8ecf1;
  position: sticky;
  top: 0;
  z-index: 10;
}
.back-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #667eea;
  padding: 4px 8px;
}
.detail-title {
  font-size: 16px;
  font-weight: 600;
}
.loading-text, .empty-text {
  text-align: center;
  color: #9aa6b5;
  padding: 60px 0;
}

.detail-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0;
  background: white;
  margin: 0 12px 12px 12px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.detail-left {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}
.image-carousel {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}
.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 0.3s ease;
}
.carousel-slide {
  min-width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.carousel-slide img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.no-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
  text-align: center;
  background: #f5f7fa;
}

.placeholder-title {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.4;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  font-size: 20px;
  cursor: pointer;
  z-index: 5;
}
.carousel-btn:hover { background: rgba(0, 0, 0, 0.7); }
.prev { left: 8px; }
.next { right: 8px; }
.carousel-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 5;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  transition: background 0.3s;
}
.dot.active { background: rgba(0, 0, 0, 0.6); }

.detail-right {
  padding: 16px 20px 20px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 60vh;
  overflow-y: auto;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}
.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.author-name {
  font-weight: 600;
  font-size: 15px;
}

.detail-title-text {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  line-height: 1.4;
}
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag {
  color: #667eea;
  font-size: 13px;
  background: #f0f2f5;
  padding: 2px 10px;
  border-radius: 12px;
}

.detail-body {
  max-height: 150px;
  overflow-y: auto;
  padding-right: 4px;
}
.detail-body p {
  font-size: 15px;
  line-height: 1.7;
  color: #333;
  margin: 0;
}

.detail-actions {
  display: flex;
  gap: 16px;
  border-top: 1px solid #f0f2f5;
  padding-top: 12px;
}
.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #57606a;
  padding: 4px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.like-btn:hover { background: #f0f2f5; color: #d0314e; }
.comment-btn:hover { background: #f0f2f5; color: #667eea; }
.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #ff4d4f;
  padding: 4px 8px;
  border-radius: 6px;
}

.detail-comments {
  border-top: 1px solid #f0f2f5;
  padding-top: 12px;
  max-height: 200px;
  overflow-y: auto;
}
.comment-item {
  padding: 4px 0;
  font-size: 14px;
}
.comment-user { font-weight: 600; color: #667eea; margin-right: 6px; }
.comment-content { color: #333; }
.no-comments { color: #9aa6b5; font-size: 14px; padding: 12px 0; text-align: center; }
</style>