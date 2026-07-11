<template>
  <div class="profile-container">
    <!-- 背景层 (移除透明度处理，原图显示) -->
    <div class="bg-layer"></div>

    <!-- 页面布局框架 -->
    <div class="layout-wrapper">
      
      <!-- 左侧全局导航栏 -->
      <div class="left-sidebar">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="友趣 Logo" class="app-logo" />
        </div>
        <div class="nav-links">
          <button @click="goToFriends" class="nav-btn">👥</button>
          <button @click="goToProfile" class="nav-btn active">👤</button>
          <button @click="goToCalendar" class="nav-btn">📅</button>
          <button @click="goToChat" class="nav-btn">💬</button>
          <button @click="$router.push('/home')" class="nav-btn">📋</button>
        </div>
      </div>

      <!-- 右侧核心内容区 -->
      <div class="main-content">
        
        <!-- 顶部工具栏 -->
        <div class="top-actions">
          <h2 class="page-title">{{ isOwnProfile ? '我的个人主页' : 'Ta的个人主页' }}</h2>
          <button v-if="isOwnProfile" @click="toggleEdit" class="edit-btn">
            {{ isEditing ? '✔️ 保存' : '✏️ 编辑资料' }}
          </button>
        </div>

        <div class="content-grid">
          
          <!-- ===== 左半部分：个人信息卡片 (透明64) ===== -->
          <div class="profile-card">
            <div class="avatar-wrapper">
              <div class="avatar" @click="triggerFileUpload">
                <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="头像" />
                <span v-else>{{ (userInfo.nickname || userInfo.username || '?').charAt(0) }}</span>
              </div>
              <input type="file" ref="fileInput" accept="image/*" style="display:none" @change="handleAvatarUpload" />
              <div v-if="isEditing" class="avatar-edit-hint">📷 更换</div>
            </div>

            <div class="info-details">
              <div class="form-group">
                <label>昵称</label>
                <input v-if="isEditing" v-model="editForm.nickname" placeholder="输入昵称" />
                <span v-else class="info-text nickname">{{ userInfo.nickname || userInfo.username }}</span>
              </div>
              <div class="form-group">
                <label>用户名</label>
                <span class="info-text username">@{{ userInfo.username }}</span>
              </div>
              <div class="form-group" v-if="isOwnProfile">
                <label>邮箱</label>
                <input v-if="isEditing" v-model="editForm.email" placeholder="输入邮箱" />
                <span v-else class="info-text">{{ userInfo.email || '未设置邮箱' }}</span>
              </div>
              <div class="form-group">
                <label>个人简介</label>
                <textarea v-if="isEditing" v-model="editForm.bio" placeholder="写一段个人简介..." rows="2"></textarea>
                <span v-else class="info-text bio">{{ userInfo.bio || '这个人很懒，什么都没写~' }}</span>
              </div>
              <div class="form-group">
                <label>兴趣标签</label>
                <input v-if="isEditing" v-model="editForm.interestTags" placeholder="如：旅行,美食,科技" />
                <div v-else class="tags-display">
                  <span v-for="tag in (userInfo.interestTags || '').split(',').filter(t => t.trim())" :key="tag" class="tag-pill">#{{ tag.trim() }}</span>
                  <span v-if="!userInfo.interestTags" class="info-text">未设置</span>
                </div>
              </div>
            </div>

            <!-- 统计数据 -->
            <div class="stats-grid">
              <div class="stat-item"><span class="num">{{ posts.length }}</span><span class="label">动态</span></div>
              <div class="stat-item"><span class="num">{{ commentsCount }}</span><span class="label">评论</span></div>
              <div class="stat-item"><span class="num">{{ totalLikes }}</span><span class="label">获赞</span></div>
              <div class="stat-item"><span class="num">{{ friendsCount }}</span><span class="label">好友</span></div>
            </div>
          </div>

          <!-- ===== 右半部分：内容列表 ===== -->
          <div class="content-right-panel">
            
            <!-- 上侧窄条：切换导航栏 (透明64) -->
            <div class="list-tabs-bar">
              <div 
                v-for="tab in tabs" 
                :key="tab.key"
                class="tab-item"
                :class="{ active: activeTab === tab.key }"
                @click="switchTab(tab.key)"
              >
                <span class="tab-label">{{ tab.label }}</span>
                <span v-if="isOwnProfile" class="lock-icon" @click.stop="toggleLock(tab.key)">
                  {{ isLocked(tab.key) ? '🔒' : '🔓' }}
                </span>
              </div>
            </div>

            <!-- 底部内容区 (透明背景) -->
            <div class="tab-content-box">
              
              <!-- 动态列表 (自适应卡片网格) -->
              <div v-if="activeTab === 'posts'" class="list-container card-grid">
                <div v-if="!isTabVisible('posts')" class="empty-state">🔒 该列表已隐藏</div>
                <div v-else-if="posts.length === 0" class="empty-state">暂无动态</div>
                <div v-else v-for="post in posts" :key="post.id" class="mini-post-card" @click="goToPostDetail(post.id)">
                  <div class="mini-img">
                    <img v-if="postImages(post).length > 0" :src="postImages(post)[0]" />
                    <!-- ✅ 使用标题占位图（带随机颜色 + 纯文字） -->
                    <div v-else class="card-image-placeholder" :style="{ background: getRandomPastelColor() }">
                      <span class="placeholder-title">{{ post.title || post.content }}</span>
                    </div>
                  </div>
                  <div class="mini-info">
                    <div class="mini-title">{{ post.title || post.content }}</div>
                    <div class="mini-meta">❤️ {{ post.likesCount || 0 }}  💬 {{ post.commentCount || 0 }}</div>
                  </div>
                </div>
              </div>

              <!-- 评论列表 (单条矩形) -->
              <div v-if="activeTab === 'comments'" class="list-container">
                <div v-if="!isTabVisible('comments')" class="empty-state">🔒 该列表已隐藏</div>
                <div v-else-if="commentsList.length === 0" class="empty-state">暂无评论</div>
                <div v-else v-for="comment in commentsList" :key="comment.id" class="rect-item" @click="goToPost(comment.postId)">
                  <div class="rect-content">“{{ comment.content }}”</div>
                  <div class="rect-meta">查看原动态 →</div>
                </div>
              </div>

              <!-- 赞过列表 (自适应卡片网格) -->
              <div v-if="activeTab === 'likes'" class="list-container card-grid">
                <div v-if="!isTabVisible('likes')" class="empty-state">🔒 该列表已隐藏</div>
                <div v-else-if="likedPosts.length === 0" class="empty-state">暂无赞过的动态</div>
                <div v-else v-for="post in likedPosts" :key="post.id" class="mini-post-card" @click="goToPostDetail(post.id)">
                  <div class="mini-img">
                    <img v-if="postImages(post).length > 0" :src="postImages(post)[0]" />
                    <!-- ✅ 使用标题占位图（带随机颜色 + 纯文字） -->
                    <div v-else class="card-image-placeholder" :style="{ background: getRandomPastelColor() }">
                      <span class="placeholder-title">{{ post.title || post.content }}</span>
                    </div>
                  </div>
                  <div class="mini-info">
                    <div class="mini-title">{{ post.title || post.content }}</div>
                    <div class="mini-meta">{{ post.nickname || post.username }}</div>
                  </div>
                </div>
              </div>

              <!-- 好友列表 (单条矩形) -->
              <div v-if="activeTab === 'friends'" class="list-container">
                <div v-if="!isTabVisible('friends')" class="empty-state">🔒 该列表已隐藏</div>
                <div v-else-if="friendsList.length === 0" class="empty-state">暂无好友</div>
                <div v-else v-for="friend in friendsList" :key="friend.id" class="rect-item friend-rect" @click="goToUserProfile(friend.id)">
                  <div class="rect-left"><span class="friend-avatar">{{ (friend.nickname || friend.username || '?').charAt(0) }}</span></div>
                  <div class="rect-content"><span class="friend-name">{{ friend.nickname || friend.username }}</span> <span class="friend-account">@{{ friend.username }}</span></div>
                  <button v-if="isOwnProfile" @click.stop="confirmDeleteFriend(friend.id, friend.nickname || friend.username)" class="del-rect-btn">删除</button>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { uploadFile } from '@/utils/upload'

const router = useRouter()
const route = useRoute()
const isEditing = ref(false)
const fileInput = ref(null)

const currentUserId = ref(0)
const targetUserId = ref(0)
const isOwnProfile = ref(true)

const lockStatus = ref({ posts: false, comments: false, likes: false, friends: false })
const tabs = [
  { key: 'posts', label: '📝 动态' },
  { key: 'comments', label: '💬 评论' },
  { key: 'likes', label: '❤️ 赞过' },
  { key: 'friends', label: '👥 好友' }
]
const activeTab = ref('posts')

const userInfo = ref({ id: 0, username: '', nickname: '', email: '', bio: '', avatar: '', interestTags: '', hidePosts: 0, hideComments: 0, hideLikes: 0, hideFriends: 0 })
const editForm = ref({ nickname: '', email: '', bio: '', interestTags: '' })

const posts = ref([])
const commentsList = ref([])
const likedPosts = ref([])
const friendsList = ref([])
const friendsCount = ref(0)
const commentsCount = ref(0)

const totalLikes = computed(() => posts.value.reduce((sum, post) => sum + (post.likesCount || 0), 0))

// ===== 导航 =====
const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToChat = () => router.push('/chat')
const goToPostDetail = (id) => router.push(`/post/${id}`)
const goToPost = (id) => router.push(`/post/${id}`)
const goToUserProfile = (userId) => router.push(`/profile/${userId}`)
// ✅ 新增：跳转日历
const goToCalendar = () => router.push('/calendar')

// ===== 锁逻辑 =====
const isLocked = (key) => lockStatus.value[key]
const toggleLock = async (key) => {
  if (!isOwnProfile.value) return
  lockStatus.value[key] = !lockStatus.value[key]
  const hideMap = { 'posts': 'hidePosts', 'comments': 'hideComments', 'likes': 'hideLikes', 'friends': 'hideFriends' }
  const hideKey = hideMap[key]
  const newValue = lockStatus.value[key] ? 1 : 0
  userInfo.value[hideKey] = newValue
  try {
    await request.put('/api/users/profile', { [hideKey]: newValue })
  } catch (e) { alert('更新隐私设置失败') }
}
const isTabVisible = (key) => {
  if (isOwnProfile.value) return true
  const hideMap = { 'posts': 'hidePosts', 'comments': 'hideComments', 'likes': 'hideLikes', 'friends': 'hideFriends' }
  return userInfo.value[hideMap[key]] !== 1
}

// ===== Tab切换 =====
const switchTab = (key) => {
  activeTab.value = key
  if (key === 'comments' && isTabVisible('comments')) loadCommentsList()
  if (key === 'likes' && isTabVisible('likes')) loadLikedPosts()
  if (key === 'friends' && isTabVisible('friends')) loadFriendsList()
}

// ===== 数据加载 =====
const loadUserInfo = async () => {
  try {
    const idParam = route.params.userId
    const res = idParam ? await request.get(`/api/users/${idParam}/profile`) : await request.get('/api/users/profile')
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      targetUserId.value = userInfo.value.id
      isOwnProfile.value = !idParam || parseInt(idParam) === currentUserId.value
    }
  } catch (e) { console.error(e) }
}

const toggleEdit = () => {
  if (isEditing.value) { updateProfile() } 
  else {
    editForm.value = { nickname: userInfo.value.nickname, email: userInfo.value.email, bio: userInfo.value.bio, interestTags: userInfo.value.interestTags }
    isEditing.value = true
  }
}
const updateProfile = async () => {
  try {
    const res = await request.put('/api/users/profile', editForm.value)
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      isEditing.value = false
    }
  } catch (e) { alert('保存失败') }
}

const triggerFileUpload = () => { if (isOwnProfile.value) fileInput.value?.click() }
const handleAvatarUpload = async (e) => {
  const file = e.target.files[0]; if (!file) return
  try {
    const url = await uploadFile(file)
    if (url) {
      await request.put('/api/users/profile', { avatar: url })
      userInfo.value.avatar = url
    }
  } catch (e) { alert('头像上传失败') }
}

const loadPosts = async () => {
  const userId = isOwnProfile.value ? currentUserId.value : targetUserId.value
  const res = await request.get(`/api/users/${userId}/posts`)
  if (res.data.code === 0) posts.value = res.data.data || []
}
const loadCommentsList = async () => {
  const userId = isOwnProfile.value ? currentUserId.value : targetUserId.value
  const res = await request.get(`/api/users/${userId}/comments`)
  if (res.data.code === 0) {
    commentsList.value = res.data.data || []
    commentsCount.value = commentsList.value.length
  }
}
const loadLikedPosts = async () => {
  const userId = isOwnProfile.value ? currentUserId.value : targetUserId.value
  const res = await request.get(`/api/users/${userId}/likes`)
  if (res.data.code === 0) likedPosts.value = res.data.data || []
}
const loadFriendsList = async () => {
  const userId = isOwnProfile.value ? currentUserId.value : targetUserId.value
  const res = await request.get(`/api/users/${userId}/friends`)
  if (res.data.code === 0) {
    friendsList.value = res.data.data || []
    friendsCount.value = friendsList.value.length
  }
}

const postImages = (post) => Array.isArray(post.images) ? post.images : (typeof post.images === 'string' ? post.images.split(',') : [])
const confirmDeleteFriend = async (id, name) => {
  if (!confirm(`确定要删除好友 "${name}" 吗？`)) return
  await request.delete(`/api/friends/${id}`)
  loadFriendsList()
}

// ===== 添加随机浅色背景函数（和 HomeView 一模一样） =====
const getRandomPastelColor = () => {
  const colors = ['#f0f4ff', '#f5f0ff', '#f0fff4', '#fff5f0', '#fff0f5', '#f0faff', '#f5fff0', '#fff8f0', '#f0f5ff', '#f5f0f5', '#f0fff0', '#fff5f5']
  return colors[Math.floor(Math.random() * colors.length)]
}

// ===== 初始化 =====
onMounted(() => {
  currentUserId.value = JSON.parse(localStorage.getItem('user') || '{}').id
  loadUserInfo().then(() => {
    loadPosts(); loadCommentsList(); loadLikedPosts(); loadFriendsList()
  })
})
</script>

<style>
body { margin: 0; padding: 0; }
</style>

<style scoped>
/* ============================================================ */
/* 1. 背景层 - 纯原图展示 */
.profile-container {
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
  background: rgba(237, 255, 249, 1); color: #2c3e32; border: none;
  width: 46px; height: 46px; border-radius: 14px; font-size: 22px;
  display: flex; align-items: center; justify-content: center; padding: 0; cursor: pointer; transition: all 0.2s;
}
.nav-btn:hover { transform: translateY(-2px); }
.nav-btn.active { background: #e0e7ff; }

/* ===== 右侧主布局 ===== */
.main-content {
  position: absolute; top: 0; left: 100px; right: 0; bottom: 0;
  display: flex; flex-direction: column; padding: 30px 40px 20px 30px; box-sizing: border-box;
}
.top-actions {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-shrink: 0;
}
.page-title { margin: 0; color: rgb(51, 69, 64); font-size: 20px; font-weight: 600; }
.edit-btn {
  background: rgba(237, 255, 249, 0.64); border: 1px solid rgba(128, 160, 152, 0.3);
  padding: 6px 16px; border-radius: 20px; color: rgb(51, 69, 64); font-size: 13px; cursor: pointer; transition: 0.2s;
}
.edit-btn:hover { background: rgba(237, 255, 249, 0.9); }

.content-grid { flex: 1; display: flex; gap: 24px; overflow: hidden; }

/* ============================================================ */
/* 2. 左半：个人信息卡片 (透明64) */
.profile-card {
  width: 32%; height: 100%;
  background: rgba(237, 255, 249, 0.64);
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  border-radius: 16px; padding: 24px; box-sizing: border-box;
  display: flex; flex-direction: column; border: 1px solid rgba(255, 255, 255, 0.3); overflow-y: auto;
}

.avatar-wrapper { position: relative; display: flex; justify-content: center; margin-bottom: 20px; }
.avatar {
  width: 80px; height: 80px; border-radius: 50%; background: rgba(51, 69, 64, 0.3);
  display: flex; justify-content: center; align-items: center; font-size: 30px; color: white; overflow: hidden; cursor: pointer;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-edit-hint {
  position: absolute; bottom: -4px; right: 50%; transform: translateX(50%);
  font-size: 12px; background: rgba(51, 69, 64, 0.9); padding: 2px 10px; border-radius: 10px; color: white;
}

.info-details { display: flex; flex-direction: column; gap: 12px; flex: 1; }
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 12px; color: rgb(51, 69, 64); opacity: 0.7; font-weight: 500; }
.info-text { color: rgb(51, 69, 64); font-size: 14px; }
.info-text.nickname { font-size: 18px; font-weight: 600; }
.info-text.username { font-size: 13px; color: rgb(51, 69, 64); opacity: 0.8; }

.form-group input, .form-group textarea {
  background: rgba(255, 255, 255, 0.5); border: 1px solid rgba(51, 69, 64, 0.2);
  border-radius: 8px; padding: 8px 12px; font-size: 14px; color: rgb(51, 69, 64); outline: none;
}
.form-group input:focus, .form-group textarea:focus { border-color: rgb(51, 69, 64); }

.tags-display { display: flex; flex-wrap: wrap; gap: 6px; }
.tag-pill { background: rgba(51, 69, 64, 0.1); color: rgb(51, 69, 64); padding: 2px 10px; border-radius: 12px; font-size: 12px; }

.stats-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; padding-top: 16px;
  border-top: 1px solid rgba(51, 69, 64, 0.1); margin-top: auto;
}
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-item .num { font-size: 18px; font-weight: 700; color: rgb(51, 69, 64); }
.stat-item .label { font-size: 11px; color: rgb(51, 69, 64); opacity: 0.8; }

/* ============================================================ */
/* 3. 右侧内容列表 */
.content-right-panel {
  flex: 1; height: 100%; display: flex; flex-direction: column; gap: 14px;
}

/* ----- 上：导航窄条 (透明64) ----- */
.list-tabs-bar {
  flex-shrink: 0; height: 48px;
  background: rgba(237, 255, 249, 0.64); backdrop-filter: blur(8px);
  border-radius: 16px; padding: 0 24px; display: flex; align-items: center; gap: 24px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}
.tab-item {
  display: flex; align-items: center; gap: 6px;
  color: rgb(51, 69, 64); opacity: 0.6; cursor: pointer; font-size: 14px; padding: 4px 0;
  position: relative; font-weight: 500; transition: all 0.2s;
}
.tab-item:hover { opacity: 1; }
.tab-item.active { opacity: 1; font-weight: 600; }
.tab-item.active::after {
  content: ''; position: absolute; bottom: -4px; left: 0; width: 100%; height: 2px; background: rgb(51, 69, 64);
}
.lock-icon { font-size: 12px; cursor: pointer; }

/* ----- 下：内容区（全透明） ----- */
.tab-content-box {
  flex: 1; overflow-y: auto; padding-right: 10px;
}

.list-container { display: flex; flex-direction: column; gap: 12px; }
.empty-state { text-align: center; color: rgba(51, 69, 64, 0.6); padding: 40px 0; font-size: 14px; }

/* ---------- 1. 动态 & 赞过卡片 (自适应列数) ---------- */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); /* 自动适应页面决定列数 */
  gap: 16px;
}
.mini-post-card {
  background: rgba(237, 255, 249, 0.64); border-radius: 12px; overflow: hidden;
  cursor: pointer; transition: 0.2s; display: flex; flex-direction: column;
}
.mini-post-card:hover { transform: translateY(-4px); }
.mini-img { width: 100%; aspect-ratio: 16/9; background: rgba(0,0,0,0.05); }
.mini-img img { width: 100%; height: 100%; object-fit: cover; }
.mini-img-placeholder {
  height: 100%; display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: rgb(51, 69, 64);
}
.mini-info { padding: 10px 12px; display: flex; flex-direction: column; gap: 4px; }
.mini-title { font-size: 14px; font-weight: 600; color: rgb(51, 69, 64); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mini-meta { font-size: 12px; color: rgb(51, 69, 64); opacity: 0.7; }

/* ---------- 2. 评论 & 好友 (单条矩形) ---------- */
.rect-item {
  background: rgba(237, 255, 249, 0.64); border-radius: 12px; padding: 14px 18px;
  cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: space-between;
  border: 1px solid rgba(255, 255, 255, 0.3);
}
.rect-item:hover { background: rgba(237, 255, 249, 0.9); }
.rect-content { font-size: 15px; font-weight: 500; color: rgb(51, 69, 64); }
.rect-meta { font-size: 12px; color: rgb(51, 69, 64); opacity: 0.6; }

/* 好友条 */
.friend-rect { justify-content: flex-start; gap: 12px; }
.rect-left { flex-shrink: 0; }
.friend-avatar {
  width: 36px; height: 36px; border-radius: 50%; background: rgba(51, 69, 64, 0.15);
  color: rgb(51, 69, 64); display: flex; align-items: center; justify-content: center; font-weight: 600;
}
.rect-content { display: flex; gap: 12px; align-items: center; }
.friend-name { font-weight: 600; }
.friend-account { font-size: 13px; color: rgb(51, 69, 64); opacity: 0.6; }
.del-rect-btn {
  margin-left: auto; background: none; border: none; color: rgba(51, 69, 64, 0.6);
  font-size: 13px; cursor: pointer; transition: 0.2s;
}
.del-rect-btn:hover { color: #d0314e; }
/* ✅ 占位图专门样式（完全沿用 Home 页） */
.card-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
  text-align: center;
  background: #f5f7fa; /* 会被内联颜色覆盖 */
}
.placeholder-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: #1d3326;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>