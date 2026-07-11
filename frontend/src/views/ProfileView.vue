<template>
  <div class="profile-container">
    <!-- 顶部导航 -->
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>{{ isOwnProfile ? '个人主页' : (userInfo.nickname || userInfo.username) + '的主页' }}</h1>
      <button v-if="isOwnProfile" @click="toggleEdit" class="edit-btn">
        {{ isEditing ? '保存' : '编辑' }}
      </button>
    </div>

    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="avatar-wrapper">
        <div class="avatar">
          <!-- ✅ 如果有头像图片就显示图片，否则显示首字母 -->
          <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="头像" />
          <span v-else>{{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) || '?' }}</span>
        </div>
        <div v-if="isEditing" class="avatar-upload" @click="triggerFileUpload">
          <span>📷</span>
        </div>
        <input type="file" ref="fileInput" accept="image/*" style="display:none" @change="handleAvatarUpload" />
      </div>
      <div class="user-info">
        <div class="username">
          <input v-if="isEditing" v-model="editForm.nickname" placeholder="昵称" />
          <span v-else>{{ userInfo.nickname || userInfo.username }}</span>
        </div>
        <div class="account">@{{ userInfo.username }}</div>
        <div class="email" v-if="isOwnProfile">
          <span v-if="isEditing">📧 <input v-model="editForm.email" placeholder="邮箱" /></span>
          <span v-else>📧 {{ userInfo.email || '未设置邮箱' }}</span>
        </div>
        <div class="bio">
          <textarea v-if="isEditing" v-model="editForm.bio" placeholder="写一段个人简介..." maxlength="200" rows="2" />
          <span v-else>{{ userInfo.bio || '这个人很懒，什么都没写~' }}</span>
        </div>
        <div class="interest-tags">
          <span v-if="!isEditing" class="tags-label">🏷️ 兴趣标签：</span>
          <span v-if="!isEditing">{{ userInfo.interestTags || '未设置' }}</span>
          <input v-else v-model="editForm.interestTags" placeholder="例如：美食,旅行,科技" />
        </div>        
      </div>
    </div>

    <!-- 统计数据 -->
    <div class="stats">
      <div class="stat-item" @click="switchTab('posts')">
        <span class="stat-number">{{ posts.length }}</span>
        <span class="stat-label">动态</span>
      </div>
      <div class="stat-item" @click="switchTab('comments')">
        <span class="stat-number">{{ commentsCount }}</span>
        <span class="stat-label">评论</span>
      </div>
      <div class="stat-item" @click="switchTab('likes')">
        <span class="stat-number">{{ totalLikes }}</span>
        <span class="stat-label">获赞</span>
      </div>
      <div class="stat-item" @click="switchTab('friends')">
        <span class="stat-number">{{ friendsCount }}</span>
        <span class="stat-label">好友</span>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.key"
        @click="switchTab(tab.key)"
        :class="['tab-btn', { active: activeTab === tab.key }]"
      >
        {{ tab.label }}
        <span v-if="isOwnProfile" class="lock-icon" @click.stop="toggleLock(tab.key)">
          {{ isLocked(tab.key) ? '🔒' : '🔓' }}
        </span>
      </button>
    </div>

    <!-- ===== 动态列表（卡片样式） ===== -->
    <div v-if="activeTab === 'posts' && isTabVisible('posts')" class="post-list">
      <div v-if="loading" class="status-text">加载中...</div>
      <div v-else-if="posts.length === 0" class="status-text">还没有发布过动态</div>
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
    <div v-else-if="activeTab === 'posts' && !isTabVisible('posts')" class="status-text">
      🔒 该列表已隐藏
    </div>

    <!-- ===== 评论列表（用户发表过的评论） ===== -->
    <div v-if="activeTab === 'comments' && isTabVisible('comments')" class="post-list">
      <div v-if="loadingComments" class="status-text">加载中...</div>
      <div v-else-if="commentsList.length === 0" class="status-text">还没有发表过评论</div>
      <div v-else v-for="comment in commentsList" :key="comment.id" class="comment-card">
        <div class="comment-card-header">
          <span class="comment-card-content">{{ comment.content }}</span>
          <span class="comment-card-time">{{ formatTime(comment.createdAt) }}</span>
        </div>
        <div class="comment-card-post" @click="goToPost(comment.postId)">
          查看原动态 →
        </div>
      </div>
    </div>
    <div v-else-if="activeTab === 'comments' && !isTabVisible('comments')" class="status-text">
      🔒 该列表已隐藏
    </div>

    <!-- ===== 赞过的动态 ===== -->
    <div v-if="activeTab === 'likes' && isTabVisible('likes')" class="post-list">
      <div v-if="loadingLikes" class="status-text">加载中...</div>
      <div v-else-if="likedPosts.length === 0" class="status-text">还没有赞过的动态</div>
      <div v-else v-for="post in likedPosts" :key="post.id" class="post-card">
        <div class="post-header">
          <div class="post-user-info" @click="goToUserProfile(post.userId)">
            <div class="post-avatar">
              <img v-if="getUserAvatar(post.userId, post.avatar, post.username)" 
                  :src="getUserAvatar(post.userId, post.avatar, post.username)" 
                  alt="头像" />
              <span v-else>{{ (getUserNickname(post.userId, post.nickname, post.username)).charAt(0) }}</span>
            </div>
            <div>
              <span class="post-username">{{ getUserNickname(post.userId, post.nickname, post.username) }}</span>
              <span class="post-time">{{ formatTime(post.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div class="post-actions">
          <button @click="handleLike(post.id)" class="action-btn like-btn">
            <span>{{ post.liked ? '❤️' : '🤍' }}</span>
            <span>{{ post.likesCount || 0 }}</span>
          </button>
        </div>
      </div>
    </div>
    <div v-else-if="activeTab === 'likes' && !isTabVisible('likes')" class="status-text">
      🔒 该列表已隐藏
    </div>

    <!-- ===== 好友列表 ===== -->
    <div v-if="activeTab === 'friends' && isTabVisible('friends')" class="friend-list-container">
      <div v-if="loadingFriends" class="status-text">加载中...</div>
      <div v-else-if="friendsList.length === 0" class="status-text">还没有好友</div>
      <div v-else v-for="friend in friendsList" :key="friend.id" class="friend-card">
        <div class="user-info" @click="goToUserProfile(friend.id)">
          <!-- ✅ 支持图片的头像 -->
          <div class="friend-avatar">
            <img 
              v-if="friend.avatar" 
              :src="friend.avatar" 
              alt="头像" 
            />
            <span v-else>{{ friend.nickname?.charAt(0) || friend.username?.charAt(0) || '?' }}</span>
          </div>
          <div class="user-detail">
            <div class="username">{{ friend.nickname || friend.username }}</div>
            <div class="account">@{{ friend.username }}</div>
          </div>
        </div>
        <button @click="confirmDeleteFriend(friend.id, friend.nickname || friend.username)" class="delete-friend-btn">
          删除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { formatTime } from '@/utils/dateUtils'
import CommentTree from '@/components/CommentTree.vue'
import { uploadFile } from '@/utils/upload'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

const router = useRouter()
const route = useRoute()
const loading = ref(true)
const loadingComments = ref(false)
const loadingLikes = ref(false)
const loadingFriends = ref(false)
const isEditing = ref(false)
const fileInput = ref(null)

// ===== 判断是看自己还是看别人 =====
const currentUserId = ref(0)
const targetUserId = ref(0)
const isOwnProfile = ref(true)

// ===== 锁状态 =====
const lockStatus = ref({
  posts: false,
  comments: false,
  likes: false,
  friends: false
})

// ===== Tab 配置 =====
const tabs = [
  { key: 'posts', label: '📝 动态' },
  { key: 'comments', label: '💬 评论' },
  { key: 'likes', label: '❤️ 赞过' },
  { key: 'friends', label: '👥 好友' }
]
const activeTab = ref('posts')

// ===== 用户信息 =====
const userInfo = ref({
  id: 0,
  username: '',
  nickname: '',
  email: '',
  bio: '',
  avatar: '',
  hidePosts: 0,
  hideComments: 0,
  hideLikes: 0,
  hideFriends: 0
})

const editForm = ref({
  nickname: '',
  email: '',
  bio: '',
  interestTags: '',
  hidePosts: 0,
  hideComments: 0,
  hideLikes: 0,
  hideFriends: 0
})

// ===== 数据 =====
const posts = ref([])
const commentsList = ref([])
const likedPosts = ref([])
const friendsList = ref([])
const friendsCount = ref(0)
const commentsCount = ref(0)

// ===== 统计 =====
const totalLikes = computed(() => {
  return posts.value.reduce((sum, post) => sum + (post.likesCount || 0), 0)
})

// ===== 加载当前用户 =====
const loadCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      currentUserId.value = user.id || 0
    } catch (e) {
      currentUserId.value = 0
    }
  }
}

// ===== 锁控制 =====
const isLocked = (tabKey) => {
  return lockStatus.value[tabKey] !== false
}

// ===== 锁控制（切换隐私状态） =====
const toggleLock = async (tabKey) => {
  console.log('🔒 toggleLock 被调用，tabKey:', tabKey, 'isOwnProfile:', isOwnProfile.value)
  
  if (!isOwnProfile.value) return

  // ✅ 切换本地状态（用于显示锁图标）
  lockStatus.value[tabKey] = !lockStatus.value[tabKey]

  // ✅ 构建需要更新的隐私字段
  const hideMap = {
    'posts': 'hidePosts',
    'comments': 'hideComments',
    'likes': 'hideLikes',
    'friends': 'hideFriends'
  }
  const hideKey = hideMap[tabKey]
  
  // ✅ 新值：lockStatus[tabKey] 为 true 表示上锁（隐藏），对应后端 hideXxx = 1
  const newValue = lockStatus.value[tabKey] ? 1 : 0
  
  // ✅ 更新 userInfo 中的对应字段
  userInfo.value[hideKey] = newValue

  // ✅ 调用后端接口更新用户资料
  try {
    const updateData = {
      nickname: userInfo.value.nickname,
      email: userInfo.value.email,
      bio: userInfo.value.bio,
      hidePosts: userInfo.value.hidePosts || 0,
      hideComments: userInfo.value.hideComments || 0,
      hideLikes: userInfo.value.hideLikes || 0,
      hideFriends: userInfo.value.hideFriends || 0
    }
    
    const res = await request.put('/api/users/profile', updateData)
    if (res.data.code === 0) {
      console.log(`✅ ${tabKey} 隐私设置已更新为 ${newValue === 1 ? '隐藏' : '公开'}`)
      // 更新本地存储中的用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          user[hideKey] = newValue
          localStorage.setItem('user', JSON.stringify(user))
        } catch (e) {}
      }
    } else {
      // 如果后端更新失败，回滚本地状态
      lockStatus.value[tabKey] = !lockStatus.value[tabKey]
      userInfo.value[hideKey] = lockStatus.value[tabKey] ? 1 : 0
      alert('更新隐私设置失败：' + (res.data.message || '请重试'))
    }
  } catch (error) {
    console.error('更新隐私设置失败:', error)
    // 回滚本地状态
    lockStatus.value[tabKey] = !lockStatus.value[tabKey]
    userInfo.value[hideKey] = lockStatus.value[tabKey] ? 1 : 0
    alert('更新隐私设置失败，请重试')
  }

  // ✅ 保存锁状态到本地
  localStorage.setItem('profileLockStatus', JSON.stringify(lockStatus.value))
}

// ===== 加载锁状态 =====
const loadLockStatus = () => {
  // 从 userInfo 中读取隐私设置来初始化锁状态
  // hideXxx = 1 表示隐藏（上锁），对应 lockStatus 为 true
  lockStatus.value = {
    posts: userInfo.value.hidePosts === 1,
    comments: userInfo.value.hideComments === 1,
    likes: userInfo.value.hideLikes === 1,
    friends: userInfo.value.hideFriends === 1
  }
}

// ===== Tab 可见性（隐私控制） =====
const isTabVisible = (tabKey) => {
  // 自己的主页：所有 Tab 都可见
  if (isOwnProfile.value) return true

  // 他人的主页：检查隐私设置
  const hideMap = {
    'posts': 'hidePosts',
    'comments': 'hideComments',
    'likes': 'hideLikes',
    'friends': 'hideFriends'
  }
  const hideKey = hideMap[tabKey]
  // ✅ 如果 hideKey 为 1，表示隐藏；为 0 表示公开
  // 如果 userInfo 中该字段不存在，默认为 0（公开）
  return userInfo.value[hideKey] !== 1
}

// ===== Tab 切换 =====
const switchTab = (tabKey) => {
  activeTab.value = tabKey

  // ✅ 切换时检查隐私，如果不满足条件则清空数据
  if (tabKey === 'comments') {
    if (!isTabVisible('comments')) {
      commentsList.value = []
      commentsCount.value = 0
      return
    }
    if (commentsList.value.length === 0) {
      loadCommentsList()
    }
  }
  if (tabKey === 'likes') {
    if (!isTabVisible('likes')) {
      likedPosts.value = []
      return
    }
    if (likedPosts.value.length === 0) {
      loadLikedPosts()
    }
  }
  if (tabKey === 'friends') {
    if (!isTabVisible('friends')) {
      friendsList.value = []
      friendsCount.value = 0
      return
    }
    if (friendsList.value.length === 0) {
      loadFriendsList()
    }
  }
}

// ===== 跳转到用户主页 =====
const goToUserProfile = (userId) => {
  if (!userId) return
  if (userId === currentUserId.value) {
    router.push('/profile')
  } else {
    router.push(`/profile/${userId}`)
  }
}

// ===== 加载用户信息 =====
const loadUserInfo = async () => {
  try {
    const userIdParam = route.params.userId
    const isViewingOther = userIdParam && parseInt(userIdParam) !== currentUserId.value

    if (isViewingOther) {
      targetUserId.value = parseInt(userIdParam)
      const res = await request.get(`/api/users/${targetUserId.value}/profile`)
      if (res.data.code === 0) {
        userInfo.value = {
          ...res.data.data,
          hidePosts: res.data.data.hidePosts ?? 0,
          hideComments: res.data.data.hideComments ?? 0,
          hideLikes: res.data.data.hideLikes ?? 0,
          hideFriends: res.data.data.hideFriends ?? 0
        }
        isOwnProfile.value = false
        return
      }
    }

    // 自己的主页
    const res = await request.get('/api/users/profile')
    if (res.data.code === 0) {
      userInfo.value = {
        ...res.data.data,
        hidePosts: res.data.data.hidePosts ?? 0,
        hideComments: res.data.data.hideComments ?? 0,
        hideLikes: res.data.data.hideLikes ?? 0,
        hideFriends: res.data.data.hideFriends ?? 0
      }
      targetUserId.value = userInfo.value.id
      isOwnProfile.value = true
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// ===== 加载动态 =====
const loadPosts = async () => {
  loading.value = true
  try {
    let postsData = []

    if (isOwnProfile.value) {
      const res = await request.get('/api/posts', {
        params: { page: 0, size: 20 }
      })
      if (res.data.code === 0) {
        const allPosts = res.data.data || []
        postsData = allPosts.filter(p => p.userId === targetUserId.value)
      }
    } else {
      const res = await request.get(`/api/users/${targetUserId.value}/posts`)
      if (res.data.code === 0) {
        postsData = res.data.data || []
      }
    }

    // 加载评论
    for (const post of postsData) {
      try {
        const commentsRes = await request.get(`/api/posts/${post.id}/comments`)
        const allComments = commentsRes.data.data || []
        const addReadyState = (comments) => {
          return comments.map(comment => ({
            ...comment,
            isReady: true,
            isPending: false,
            replies: comment.replies ? addReadyState(comment.replies) : []
          }))
        }
        post.comments = addReadyState(allComments)
      } catch (e) {
        post.comments = []
      }
    }

    // ✅ 确保 tags 是数组
    posts.value = postsData.map(post => ({
      ...post,
      tags: post.tags || []
    }))
  } catch (error) {
    console.error('加载动态失败:', error)
  } finally {
    loading.value = false
  }
}

// ===== 加载评论列表 =====
const loadCommentsList = async () => {
  // ✅ 先检查隐私设置
  if (!isTabVisible('comments')) {
    commentsList.value = []
    commentsCount.value = 0
    return
  }

  loadingComments.value = true
  try {
    let res
    if (isOwnProfile.value) {
      res = await request.get('/api/users/comments')
    } else {
      res = await request.get(`/api/users/${targetUserId.value}/comments`)
    }
    if (res.data.code === 0) {
      commentsList.value = res.data.data || []
      commentsCount.value = commentsList.value.length
    }
  } catch (error) {
    console.error('加载评论列表失败:', error)
    commentsList.value = []
    commentsCount.value = 0
  } finally {
    loadingComments.value = false
  }
}

// ===== 加载赞过的动态 =====
const loadLikedPosts = async () => {
  if (!isTabVisible('likes')) {
    likedPosts.value = []
    return
  }

  loadingLikes.value = true
  try {
    let res
    // ✅ 统一使用 /api/users/{userId}/likes 接口
    // 自己的主页也使用这个接口，传入自己的 userId
    const userId = isOwnProfile.value ? currentUserId.value : targetUserId.value
    res = await request.get(`/api/users/${userId}/likes`)
    
    if (res.data.code === 0) {
      likedPosts.value = (res.data.data || []).map(post => ({
        ...post,
        nickname: post.nickname || post.username || '匿名用户',
        username: post.username || '匿名用户',
        avatar: post.avatar || null
      }))
      console.log('✅ 赞过列表:', likedPosts.value)
    }
  } catch (error) {
    console.error('加载赞过的动态失败:', error)
    likedPosts.value = []
  } finally {
    loadingLikes.value = false
  }
}

// ===== 加载好友列表 =====
const loadFriendsList = async () => {
  if (!isTabVisible('friends')) {
    friendsList.value = []
    friendsCount.value = 0
    return
  }

  loadingFriends.value = true
  try {
    let res
    if (isOwnProfile.value) {
      res = await request.get('/api/friends')
    } else {
      res = await request.get(`/api/users/${targetUserId.value}/friends`)
    }
    if (res.data.code === 0) {
      friendsList.value = res.data.data || []
      friendsCount.value = friendsList.value.length  // ✅ 确保更新
    }
  } catch (error) {
    console.error('加载好友列表失败:', error)
    friendsList.value = []
    friendsCount.value = 0
  } finally {
    loadingFriends.value = false
  }
}

// ===== 点赞功能 =====
const handleLike = async (postId) => {
  try {
    const res = await request.post(`/api/posts/${postId}/like`)
    if (res.data.code === 0) {
      const post = posts.value.find(p => p.id === postId)
      if (post) {
        post.liked = !post.liked
        post.likesCount = res.data.data
      }
      const likedPost = likedPosts.value.find(p => p.id === postId)
      if (likedPost) {
        likedPost.liked = !likedPost.liked
        likedPost.likesCount = res.data.data
      }
    }
  } catch (error) {
    console.error('点赞失败', error)
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

// ===== 删除动态 =====
const confirmDeletePost = (postId) => {
  if (confirm('确定要删除这条动态吗？')) {
    deletePost(postId)
  }
}

const deletePost = async (postId) => {
  try {
    const res = await request.delete(`/api/posts/${postId}`)
    if (res.data.code === 0) {
      posts.value = posts.value.filter(p => p.id !== postId)
      alert('动态已删除')
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除动态失败:', error)
    alert('删除失败，请重试')
  }
}

// ===== 跳转到动态详情 =====
const goToPost = (postId) => {
  router.push(`/post/${postId}`)
}

// ===== 保存个人资料 =====
const updateProfile = async (data) => {
  try {
    // ✅ 确保 interestTags 字段存在
    const payload = {
      nickname: data.nickname || userInfo.value.nickname,
      email: data.email || userInfo.value.email,
      bio: data.bio || userInfo.value.bio,
      interestTags: data.interestTags || userInfo.value.interestTags || '',
      hidePosts: data.hidePosts !== undefined ? data.hidePosts : userInfo.value.hidePosts,
      hideComments: data.hideComments !== undefined ? data.hideComments : userInfo.value.hideComments,
      hideLikes: data.hideLikes !== undefined ? data.hideLikes : userInfo.value.hideLikes,
      hideFriends: data.hideFriends !== undefined ? data.hideFriends : userInfo.value.hideFriends
    }
    
    const res = await request.put('/api/users/profile', payload)
    console.log('📤 发送的数据:', payload)
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      alert('保存成功！')
      isEditing.value = false
    } else {
      alert(res.data.message || '保存失败')
    }
  } catch (error) {
    console.error('更新资料失败:', error)
    alert('更新失败，请重试')
  }
}

// ===== 其他方法 =====
const goBack = () => router.push('/home')
const goChat = (friendId) => router.push(`/chat/${friendId}`)

const toggleEdit = () => {
  if (isEditing.value) {
    // ✅ 保存时确保 interestTags 有值
    const payload = {
      nickname: editForm.value.nickname || userInfo.value.nickname,
      email: editForm.value.email || userInfo.value.email,
      bio: editForm.value.bio || userInfo.value.bio,
      interestTags: editForm.value.interestTags || '',  // ✅ 确保有值
      hidePosts: editForm.value.hidePosts ? 1 : 0,
      hideComments: editForm.value.hideComments ? 1 : 0,
      hideLikes: editForm.value.hideLikes ? 1 : 0,
      hideFriends: editForm.value.hideFriends ? 1 : 0
    }
    updateProfile(payload)
  } else {
    // ✅ 进入编辑模式时填充当前值
    editForm.value.nickname = userInfo.value.nickname || ''
    editForm.value.email = userInfo.value.email || ''
    editForm.value.bio = userInfo.value.bio || ''
    editForm.value.interestTags = userInfo.value.interestTags || ''  // ✅ 关键
    editForm.value.hidePosts = userInfo.value.hidePosts || 0
    editForm.value.hideComments = userInfo.value.hideComments || 0
    editForm.value.hideLikes = userInfo.value.hideLikes || 0
    editForm.value.hideFriends = userInfo.value.hideFriends || 0
    isEditing.value = true
  }
}

const triggerFileUpload = () => fileInput.value?.click()
// ===== 上传头像 =====
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请上传图片文件')
    return
  }

  // 验证文件大小（限制2MB）
  if (file.size > 2 * 1024 * 1024) {
    alert('图片大小不能超过2MB')
    return
  }

  try {
    // 1. 上传头像到服务器
    const avatarUrl = await uploadFile(file)
    if (!avatarUrl) {
      alert('头像上传失败，请重试')
      return
    }

    // 2. 更新用户资料中的头像字段
    const res = await request.put('/api/users/profile', {
      avatar: avatarUrl
    })

    if (res.data.code === 0) {
      // 更新本地用户信息
      userInfo.value.avatar = avatarUrl
      
      // 更新 localStorage 中的用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          user.avatar = avatarUrl
          localStorage.setItem('user', JSON.stringify(user))
        } catch (e) {}
      }
      
      alert('头像更新成功！')
    } else {
      alert(res.data.message || '更新失败')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    alert('头像上传失败，请重试')
  }
}

// ===== 加载评论数（只获取数量，不加载列表） =====
const loadCommentsCount = async () => {
  try {
    let res
    if (isOwnProfile.value) {
      res = await request.get('/api/users/comments')
    } else {
      res = await request.get(`/api/users/${targetUserId.value}/comments`)
    }
    if (res.data.code === 0) {
      commentsCount.value = (res.data.data || []).length
    }
  } catch (error) {
    console.error('加载评论数失败:', error)
    commentsCount.value = 0
  }
}

// ===== 图片处理 =====
const postImages = (post) => {
  if (!post.images) return []
  if (Array.isArray(post.images)) return post.images
  if (typeof post.images === 'string') return post.images.split(',').filter(url => url.trim())
  return []
}

// ===== 随机浅色背景 =====
const getRandomPastelColor = () => {
  const colors = ['#f0f4ff', '#f5f0ff', '#f0fff4', '#fff5f0', '#fff0f5', '#f0faff', '#f5fff0', '#fff8f0', '#f0f5ff', '#f5f0f5', '#f0fff0', '#fff5f5']
  return colors[Math.floor(Math.random() * colors.length)]
}

// ===== 跳转到动态详情 =====
const goToPostDetail = (postId) => {
  if (!postId) return
  router.push(`/post/${postId}`)
}

// ===== 删除好友 =====
const confirmDeleteFriend = (friendId, friendName) => {
  if (confirm(`确定要删除好友 "${friendName}" 吗？`)) {
    deleteFriend(friendId)
  }
}

const deleteFriend = async (friendId) => {
  try {
    const res = await request.delete(`/api/friends/${friendId}`)
    if (res.data.code === 0) {
      // 从列表中移除
      friendsList.value = friendsList.value.filter(f => f.id !== friendId)
      friendsCount.value = friendsList.value.length
      alert('已删除好友')
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除好友失败:', error)
    alert('删除失败，请重试')
  }
}

// ===== 初始化 =====
onMounted(async () => {
  loadCurrentUser()
  loadLockStatus()
  await loadUserInfo()
  await loadPosts()
  
  // ✅ 初始化加载评论数和好友数
  await loadCommentsCount()
  await loadFriendsList()
  
  // 根据是否是自己的主页决定加载哪些数据
  if (isOwnProfile.value) {
    await loadCommentsList()
    await loadLikedPosts()
  }
})
</script>

<style scoped>
.profile-container {
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
.edit-btn { background: none; border: none; font-size: 14px; cursor: pointer; color: #667eea; padding: 4px 8px; font-weight: 500; }

.profile-card {
  background: white;
  border-radius: 16px;
  padding: 24px 20px;
  margin: 16px 0 20px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar-wrapper { position: relative; flex-shrink: 0; }
.avatar {
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white; display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 600;
}
.avatar-upload {
  position: absolute; bottom: 0; right: 0;
  background: #667eea; color: white; border-radius: 50%;
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; cursor: pointer; border: 2px solid white;
}
.user-info { flex: 1; }
.user-info .username { font-size: 20px; font-weight: 600; color: #1f2a3a; }
.user-info .account { font-size: 14px; color: #9aa6b5; margin: 2px 0 6px 0; }
.user-info .bio { font-size: 14px; color: #57606a; }
.user-info .email { font-size: 14px; color: #57606a; margin: 2px 0 4px 0; }

.stats {
  display: flex;
  justify-content: space-around;
  background: white;
  border-radius: 12px;
  padding: 14px 0;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}
.stat-item { display: flex; flex-direction: column; align-items: center; cursor: pointer; }
.stat-number { font-size: 20px; font-weight: 700; color: #1f2a3a; }
.stat-label { font-size: 13px; color: #9aa6b5; }

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  background: white;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
}
.tab-btn {
  flex: 1;
  min-width: 60px;
  padding: 10px 12px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  background: transparent;
  color: #666;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}
.tab-btn:hover:not(.active) { background: #f0f2f5; }

.lock-icon {
  font-size: 14px;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
  padding: 0 2px;
  user-select: none;
}
.lock-icon:hover { opacity: 1; }
.tab-btn.active .lock-icon { color: rgba(255,255,255,0.8); }

.post-list { display: flex; flex-direction: column; gap: 12px; }
.post-card {
  background: white;
  border-radius: 12px;
  padding: 14px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.post-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.post-content { font-size: 15px; color: #24292e; line-height: 1.6; margin-bottom: 10px; }
.post-actions { display: flex; gap: 20px; border-top: 1px solid #f0f2f5; padding-top: 10px; }
.action-btn {
  background: none; border: none; cursor: pointer;
  font-size: 14px; color: #57606a; padding: 4px 8px;
  border-radius: 6px; display: flex; align-items: center; gap: 4px;
}
.like-btn:hover { background: #f0f2f5; color: #d0314e; }
.comment-btn:hover { background: #f0f2f5; color: #667eea; }

.delete-post-btn {
  background: none; border: none; cursor: pointer;
  font-size: 16px; color: #9aa6b5; padding: 4px 8px;
  border-radius: 6px; transition: all 0.2s;
}
.delete-post-btn:hover { background: #f0f2f5; color: #ff4d4f; }

.status-text { text-align: center; color: #9aa6b5; padding: 40px 0; font-size: 15px; }

/* ===== 评论卡片 ===== */
.comment-card {
  background: white;
  border-radius: 12px;
  padding: 14px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 10px;
}
.comment-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.comment-card-content { font-size: 15px; color: #24292e; flex: 1; }
.comment-card-time { font-size: 12px; color: #9aa6b5; white-space: nowrap; }
.comment-card-post {
  margin-top: 8px;
  font-size: 13px;
  color: #667eea;
  cursor: pointer;
  text-align: right;
}
.comment-card-post:hover { text-decoration: underline; }

/* ===== 好友列表 ===== */
.friend-list-container { display: flex; flex-direction: column; gap: 10px; }
.friend-card {
  display: flex; justify-content: space-between; align-items: center;
  background: white; padding: 12px 16px; border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.friend-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white; display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 600; flex-shrink: 0; overflow: hidden; 
}
.friend-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;  /* ✅ 图片裁剪填充圆形 */
}
.user-detail .username { font-weight: 600; font-size: 15px; }
.user-detail .account { font-size: 12px; color: #9aa6b5; }
.chat-btn {
  padding: 6px 14px; border: none; border-radius: 16px;
  background: #667eea; color: white; cursor: pointer; font-size: 13px;
}
.chat-btn:hover { opacity: 0.9; }

.post-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.post-user-info:hover .post-username { color: #667eea; }
.post-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.post-username { font-weight: 600; font-size: 15px; color: #1f2a3a; margin-right: 8px; transition: color 0.2s; }
.post-time { color: #9aa6b5; font-size: 12px; }
.comments-section { margin-top: 10px; padding-top: 10px; border-top: 1px solid #f0f2f5; }

.image-upload {
  margin: 12px 0;
}

.upload-btn {
  padding: 8px 16px;
  border: 1px dashed #d0d7de;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  color: #555;
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-img {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  cursor: pointer;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  overflow: hidden;  /* ✅ 确保图片不超出圆角 */
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.privacy-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.privacy-checkboxes label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: normal;
  color: #333;
  cursor: pointer;
}
.privacy-checkboxes input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.post-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.interest-tags {
  margin-top: 6px;
  font-size: 14px;
  color: #57606a;
}
.interest-tags input {
  font-size: 14px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  padding: 4px 8px;
  width: 100%;
  max-width: 300px;
}
.tags-label { font-weight: 500; color: #333; }

/* ===== 卡片式动态列表 ===== */
.post-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
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

/* ===== 删除好友按钮 ===== */
.delete-friend-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 16px;
  background: #ff4d4f;
  color: white;
  cursor: pointer;
  font-size: 13px;
  flex-shrink: 0;
  transition: background 0.2s;
}

.delete-friend-btn:hover {
  background: #e04345;
}
</style>