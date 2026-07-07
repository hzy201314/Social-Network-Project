<template>
  <div class="home-container">
    <!-- 顶部导航 -->
    <div class="header">
      <h1>🌐 社交圈</h1>
      <div class="header-actions">
        <button @click="goToPublish" class="publish-btn">+ 发布</button>
        <button @click="handleLogout" class="logout-btn">退出</button>
      </div>
    </div>

    <!-- 动态列表 -->
    <div class="post-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">暂无动态，快来发布第一条吧！</div>
      <div v-else v-for="post in posts" :key="post.id" class="post-card">
        <div class="post-header">
          <span class="username">{{ post.username || '匿名用户' }}</span>
          <span class="time">{{ post.createdAt || '刚刚' }}</span>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div class="post-actions">
          <button @click="handleLike(post.id)" class="like-btn">
            <span>{{ post.liked ? '❤️' : '🤍' }}</span> {{ post.likeCount || 0 }}
          </button>
          <button @click="toggleComment(post.id)" class="comment-btn">
            💬 {{ post.commentCount || 0 }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const posts = ref([])
const loading = ref(true)

// 模拟数据（后端好了替换成真实请求）
const loadPosts = async () => {
  loading.value = true
  try {
    // 模拟请求延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    posts.value = [
      { id: 1, username: '小向', content: '今天天气真好！☀️', likeCount: 3, liked: false, commentCount: 1, createdAt: '2024-01-15 10:30' },
      { id: 2, username: '小胡', content: '终于把登录功能调通了！🎉', likeCount: 5, liked: false, commentCount: 2, createdAt: '2024-01-15 09:00' }
    ]
  } catch (error) {
    console.error('加载动态失败', error)
  } finally {
    loading.value = false
  }
}

const goToPublish = () => {
  router.push('/publish')
}

const handleLogout = () => {
  router.push('/login')
}

const handleLike = (postId) => {
  const post = posts.value.find(p => p.id === postId)
  if (!post) return
  if (post.liked) {
    post.likeCount--
    post.liked = false
  } else {
    post.likeCount++
    post.liked = true
  }
}

const toggleComment = (postId) => {
  alert('评论功能开发中...')
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
/* 整个页面的背景色，更柔和 */
.home-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 16px 80px 16px;  /* 底部留白更多 */
  background-color: #f5f7fa;  /* 浅灰蓝底色，更现代 */
  min-height: 100vh;
}

/* ===== 好看的顶栏 ===== */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 12px 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #e8ecf1; /* 更浅的边框 */
}

.header h1 {
  font-size: 24px;  /* 字号稍微大一点 */
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;  /* 让文字变成渐变色，需要配合下一行 */
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 顶栏按钮组 */
.header-actions {
  display: flex;
  gap: 10px;
}

.publish-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 8px 18px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);  /* 加了一点和主题色匹配的阴影 */
  transition: transform 0.1s ease;
}
.publish-btn:hover {
  transform: scale(1.02);  /* 鼠标悬停时稍微放大一点点，更有反馈感 */
}

.logout-btn {
  background: #ffffff;
  border: 1px solid #d0d7de;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  transition: all 0.1s ease;
}
.logout-btn:hover {
  background: #f0f0f0;
  border-color: #b0b8c0;
}

/* ===== 动态卡片 ===== */
.post-card {
  background: white;
  border-radius: 16px;  /* 更圆润 */
  padding: 18px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);  /* 更轻、更柔和的阴影 */
  transition: box-shadow 0.2s ease;
  border: 1px solid #ffffff; /* 留一个小白边，让卡片和背景区分开 */
}
.post-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.username {
  font-weight: 600;
  font-size: 15px;
  color: #1f2a3a;
}

.time {
  color: #9aa6b5;
  font-size: 12px;
}

.post-content {
  font-size: 15px;
  line-height: 1.7;
  margin-bottom: 14px;
  color: #24292e;
}

/* ===== 底部互动按钮 ===== */
.post-actions {
  display: flex;
  gap: 20px;
  border-top: 1px solid #f0f2f5;
  padding-top: 12px;
}

.like-btn, .comment-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #57606a;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s ease, color 0.15s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}
.like-btn:hover {
  background: #f0f2f5;
  color: #d0314e; /* 点赞时显示红色调，更有感觉 */
}
.comment-btn:hover {
  background: #f0f2f5;
  color: #667eea;
}

.loading, .empty {
  text-align: center;
  color: #9aa6b5;
  padding: 60px 0;
  font-size: 15px;
}
</style>