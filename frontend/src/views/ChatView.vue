<template>
  <div class="home-container">
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
          <button @click="goToChat" class="nav-btn active">💬</button>
          <button @click="$router.push('/home')" class="nav-btn">📋</button>
        </div>
      </div>

      <!-- 2. 聊天页面主布局 -->
      <div class="chat-layout">
        
        <!-- 左侧：好友/群聊列表 (合并) -->
        <div class="contact-list-wrapper">
          <h2 class="list-title">消息</h2>
          
          <div class="contact-items">
            <div v-if="loadingContacts" class="status-text">加载中...</div>
            <div v-else-if="contacts.length === 0" class="status-text">暂无联系人或群聊</div>
            
            <div 
              v-else 
              v-for="item in filteredContacts" 
              :key="item.id"
              class="contact-item"
              :class="{ active: currentContactId === item.id }"
              @click="selectContact(item)"
            >
              <!-- 左侧列表头像 -->
              <div class="contact-avatar-wrapper">
                <div v-if="item.type === 'group'" class="contact-avatar group-avatar">👥</div>
                <div v-else class="contact-avatar">
                  <img 
                    v-if="item.avatar" 
                    :src="item.avatar" 
                    alt="头像" 
                  />
                  <span v-else>{{ (item.nickname || item.username || '?').charAt(0).toUpperCase() }}</span>
                </div>
                <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount }}</span>
              </div>

              <div class="contact-info">
                <div class="contact-name">{{ item.type === 'group' ? item.name : (item.nickname || item.username) }}</div>
                <div class="contact-msg">{{ item.lastMessage || '暂无消息' }}</div>
              </div>
              <div class="contact-time">{{ formatListTime(item.lastTime) }}</div>
            </div>
          </div>
        </div>

        <!-- 右侧：聊天框区域 -->
        <div class="chat-area-wrapper">
          <!-- 聊天框头 -->
          <div class="chat-header">
            <span class="chat-title">
              {{ currentContact?.type === 'group' ? currentContact?.name : (currentContact?.nickname || currentContact?.username || '选择联系人') }}
            </span>
          </div>

          <!-- 消息展示区 -->
          <div class="message-list" ref="messageListRef">
            <div v-if="loadingMessages" class="status-text">加载历史消息中...</div>
            <div v-else-if="messages.length === 0" class="status-text">暂无消息，打个招呼吧！</div>
            <div 
              v-else 
              v-for="msg in messages" 
              :key="msg.id"
              class="message-item"
              :class="{ 'message-self': msg.senderId === currentUserId }"
            >
              <!-- ===== 这里重点修复：聊天框内消息头像 ===== -->
              <div class="message-avatar-wrapper">
                <div class="message-avatar">
                  <!-- 如果发送者有头像图片，展示图片 -->
                  <img 
                    v-if="msg.senderAvatar" 
                    :src="msg.senderAvatar" 
                    alt="头像"
                  />
                  <!-- 否则展示首字母 -->
                  <span v-else>{{ (msg.senderNickname || msg.senderUsername || '?').charAt(0).toUpperCase() }}</span>
                </div>
              </div>

              <div class="message-body">
                <div class="message-bubble">
                  
                  <!-- 纯文本消息 -->
                  <div v-if="(!msg.fileType || msg.fileType === 'text') && !msg.fileUrl" class="message-text">{{ msg.content }}</div>
                  
                  <!-- 图片消息 -->
                  <div v-else-if="msg.fileType === 'image' || msg.content?.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i)" class="message-image">
                    <img :src="msg.fileUrl || msg.content" @click="previewImage(msg.fileUrl || msg.content)" alt="图片" />
                  </div>
                  
                  <!-- 文件/链接消息 (点击即跳转) -->
                  <div v-else-if="msg.fileUrl || msg.content?.startsWith('/uploads/') || msg.content?.startsWith('http')" class="message-file" @click="handleFileClick(msg)">
                    <span>📎</span>
                    <span>{{ msg.fileName || msg.content?.split('/').pop() || '文件' }}</span>
                    <span class="file-size" v-if="msg.fileSize">{{ formatFileSize(msg.fileSize) }}</span>
                  </div>
                  
                  <!-- 兜底 -->
                  <div v-else class="message-text">{{ msg.content }}</div>
                  
                  <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部输入区 -->
          <div class="input-area">
            <div class="input-tools">
              <button @click="triggerFileUpload('image')" class="tool-btn">🖼️</button>
              <button @click="triggerFileUpload('file')" class="tool-btn">📎</button>
              <button @click="triggerFileUpload('video')" class="tool-btn">🎬</button>
            </div>
            
            <div class="input-row">
              <input 
                v-model="inputMessage" 
                @keyup.enter="sendMessage" 
                placeholder="输入消息..."
              />
              <button @click="sendMessage" class="send-btn">确认</button>
            </div>

            <!-- 待上传文件预览 -->
            <div v-if="pendingFiles.length > 0" class="file-preview">
              <div v-for="(file, index) in pendingFiles" :key="index" class="file-preview-item">
                <span>{{ file.name }}</span>
                <button @click="removePendingFile(index)">✕</button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { uploadFile } from '@/utils/upload'

const router = useRouter()
const currentUserId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)

// ===== 状态 =====
const contacts = ref([])
const currentContactId = ref(null)
const currentContactType = ref(null) // 'friend' or 'group'
const messages = ref([])
const inputMessage = ref('')
const messageListRef = ref(null)
const loadingContacts = ref(false)
const loadingMessages = ref(false)
const pendingFiles = ref([])
const previewImageUrl = ref(null)
const searchKeyword = ref('')

let pollingTimer = null

// ===== 导航跳转 =====
const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToCalendar = () => router.push('/calendar')
const goToChat = () => router.push('/chat')

// ===== 计算属性 =====
const currentContact = computed(() => {
  return contacts.value.find(c => c.id === currentContactId.value && c.type === currentContactType.value)
})

const filteredContacts = computed(() => {
  if (!searchKeyword.value) return contacts.value
  const kw = searchKeyword.value.toLowerCase()
  return contacts.value.filter(item => {
    const name = item.type === 'group' ? item.name : (item.nickname || item.username)
    return name?.toLowerCase().includes(kw)
  })
})

// ===== 加载联系人(合并好友和群聊) =====
const loadContacts = async () => {
  loadingContacts.value = true
  try {
    const [friendRes, groupRes] = await Promise.all([
      request.get('/api/friends'),
      request.get('/api/groups')
    ])
    
    let friendList = friendRes.data.code === 0 ? (friendRes.data.data || []) : []
    let groupList = groupRes.data.code === 0 ? (groupRes.data.data || []) : []

    const formattedFriends = friendList.map(f => ({
      ...f,
      type: 'friend',
      unreadCount: 0,
      lastMessage: '',
      lastTime: '',
      _lastMsgTime: 0
    }))

    const formattedGroups = groupList.map(g => ({
      ...g,
      type: 'group',
      unreadCount: 0,
      lastMessage: '',
      lastTime: '',
      _lastMsgTime: 0
    }))

    contacts.value = [...formattedFriends, ...formattedGroups]
    
    await loadLatestMessagesForAllContacts()
    sortContacts()

    if (contacts.value.length > 0 && !currentContactId.value) {
      const first = contacts.value[0]
      currentContactId.value = first.id
      currentContactType.value = first.type
      loadMessages(first.id, first.type)
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
  } finally {
    loadingContacts.value = false
  }
}

// ===== 排序联系人 =====
const sortContacts = () => {
  contacts.value.sort((a, b) => (b._lastMsgTime || 0) - (a._lastMsgTime || 0))
}

// ===== 批量加载最后一条消息 =====
const loadLatestMessagesForAllContacts = async () => {
  const promises = contacts.value.map(async (contact) => {
    try {
      const url = contact.type === 'group' 
        ? `/api/groups/${contact.id}/messages` 
        : `/api/messages/${contact.id}`
      
      const res = await request.get(url)
      if (res.data.code === 0 && res.data.data && res.data.data.length > 0) {
        const latestMsg = res.data.data[res.data.data.length - 1]
        contact.lastMessage = latestMsg.content || (latestMsg.fileUrl ? '[文件]' : '')
        contact.lastTime = latestMsg.createdAt
        contact._lastMsgTime = new Date(latestMsg.createdAt).getTime()
      }
    } catch (e) { /* 忽略错误 */ }
  })
  await Promise.all(promises)
}

// ===== 加载聊天记录 =====
const loadMessages = async (contactId, type) => {
  if (!contactId) return
  loadingMessages.value = true
  try {
    const url = type === 'group' 
      ? `/api/groups/${contactId}/messages` 
      : `/api/messages/${contactId}`
      
    const res = await request.get(url)
    if (res.data.code === 0) {
      messages.value = (res.data.data || []).map(msg => ({
        ...msg,
        fileType: msg.fileType || msg.type,
        fileUrl: msg.fileUrl || (msg.content && msg.content.startsWith('/uploads/') ? msg.content : null),
        // 确保取到发送者的头像
        senderAvatar: msg.senderAvatar || msg.avatar || null
      }))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loadingMessages.value = false
  }
}

// ===== 选择联系人 =====
const selectContact = (item) => {
  if (currentContactId.value === item.id && currentContactType.value === item.type) return
  currentContactId.value = item.id
  currentContactType.value = item.type
  messages.value = []
  loadMessages(item.id, item.type)
}

// ===== 发送消息 =====
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content && pendingFiles.value.length === 0) return

  const contact = currentContact.value
  if (!contact) return

  let uploadedFile = null
  if (pendingFiles.value.length > 0) {
    const file = pendingFiles.value[0]
    const url = await uploadFile(file)
    if (url) {
      let fileType = 'file'
      if (file.type.startsWith('image/')) fileType = 'image'
      else if (file.type.startsWith('video/')) fileType = 'video'
      uploadedFile = { url, name: file.name, type: fileType, size: file.size }
    }
    pendingFiles.value = []
  }

  try {
    let res
    const payload = {
      receiverId: contact.id,
      content: content || (uploadedFile ? uploadedFile.url : ''),
      fileUrl: uploadedFile ? uploadedFile.url : null,
      fileType: uploadedFile ? uploadedFile.type : null,
      fileName: uploadedFile ? uploadedFile.name : null,
      fileSize: uploadedFile ? uploadedFile.size : 0
    }

    if (contact.type === 'group') {
      res = await request.post(`/api/groups/${contact.id}/messages`, payload)
    } else {
      res = await request.post('/api/messages/send', payload)
    }

    if (res.data.code === 0) {
      inputMessage.value = ''
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      const newMsg = {
        ...res.data.data,
        senderId: currentUserId.value,
        senderAvatar: user.avatar || null, // 把自己刚刚发的消息头像加上
        fileUrl: uploadedFile ? uploadedFile.url : null,
        fileType: uploadedFile ? uploadedFile.type : null,
        fileName: uploadedFile ? uploadedFile.name : null,
        createdAt: new Date().toISOString()
      }
      messages.value.push(newMsg)
      
      const contactInList = contacts.value.find(c => c.id === contact.id && c.type === contact.type)
      if (contactInList) {
        contactInList.lastMessage = content || (uploadedFile ? '[文件]' : '')
        contactInList.lastTime = new Date().toISOString()
        contactInList._lastMsgTime = new Date().getTime()
        sortContacts()
      }
      await scrollToBottom()
    }
  } catch (error) {
    console.error('发送失败:', error)
  }
}

// ===== 轮询接收新消息 =====
const pollNewMessages = async () => {
  if (!currentContactId.value || !currentContactType.value) return
  try {
    const url = currentContactType.value === 'group' 
      ? `/api/groups/${currentContactId.value}/messages` 
      : `/api/messages/${currentContactId.value}`
      
    const res = await request.get(url)
    if (res.data.code === 0) {
      const rawMessages = res.data.data || []
      const processedMsgs = rawMessages.map(msg => ({
        ...msg,
        fileType: msg.fileType || msg.type,
        fileUrl: msg.fileUrl || (msg.content && msg.content.startsWith('/uploads/') ? msg.content : null),
        senderAvatar: msg.senderAvatar || msg.avatar || null
      }))
      
      if (processedMsgs.length > messages.value.length) {
        const newMessages = processedMsgs.slice(messages.value.length)
        messages.value.push(...newMessages)
        await scrollToBottom()
      }
    }
  } catch (error) {
    // 静默处理轮询失败
  }
}

// ===== 文件上传相关 =====
const triggerFileUpload = (type) => {
  const input = document.createElement('input')
  input.type = 'file'
  if (type === 'image') input.accept = 'image/*'
  else if (type === 'video') input.accept = 'video/*'
  else input.accept = '.pdf,.doc,.docx,.xls,.xlsx,.txt'
  
  input.onchange = (e) => {
    const files = Array.from(e.target.files)
    pendingFiles.value.push(...files)
  }
  input.click()
}
const removePendingFile = (index) => {
  pendingFiles.value.splice(index, 1)
}

// ===== 工具方法 =====
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatListTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  return date.toLocaleDateString('zh-CN')
}

const formatFileSize = (bytes) => {
  if (!bytes) return ''
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i]
}

const previewImage = (url) => {
  previewImageUrl.value = url
}
const handleFileClick = (msg) => {
  const url = msg.fileUrl || msg.content
  if (!url) return
  window.open(url, '_blank')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadContacts()
  pollingTimer = setInterval(pollNewMessages, 3000)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style>
body { margin: 0; padding: 0; }
</style>

<style scoped>
/* ============================================================ */
/* 1. 背景层与结构 */
.home-container {
  width: 100vw; height: 100vh; position: relative; margin: 0; padding: 0; overflow: hidden;
}
.bg-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background-image: url('@/assets/chat-bg.png');
  background-size: cover; background-position: center;
  background-color: rgba(255, 255, 255, 0.58);
  background-blend-mode: overlay; 
  z-index: 1;
}
.layout-wrapper { width: 100%; height: 100%; position: absolute; top: 0; left: 0; z-index: 2; }

/* ============================================================ */
/* 2. 导航栏 */
.left-sidebar {
  position: absolute; top: 0; left: 0; width: 100px; height: 100vh;
  background: rgba(35, 63, 47, 0.64); z-index: 10;
  display: flex; flex-direction: column; align-items: center; padding-top: 18px;
}
.logo-wrapper { width: 50px; height: 50px; display: flex; align-items: center; justify-content: center; margin-bottom: 40px; flex-shrink: 0; }
.app-logo { width: 100%; height: 100%; object-fit: contain; display: block; }
.nav-links { display: flex; flex-direction: column; align-items: center; gap: 24px; }
.nav-btn {
  background: rgba(237, 255, 249, 0.45); color: #2c3e32; border: none;
  width: 46px; height: 46px; border-radius: 14px; font-size: 22px;
  display: flex; align-items: center; justify-content: center; padding: 0; cursor: pointer; transition: all 0.2s;
}
.nav-btn:hover { transform: translateY(-2px); }
.nav-btn.active { background: rgba(237, 255, 249, 0.8); }

/* ============================================================ */
/* 3. 聊天布局 */
.chat-layout {
  position: absolute; top: 0; left: 100px; right: 0; bottom: 0;
  display: flex; flex-direction: row;
}

/* 左侧好友列表 */
.contact-list-wrapper {
  width: 30%; height: 100%; padding: 40px 20px 20px 30px; box-sizing: border-box;
  display: flex; flex-direction: column; background: rgba(150, 255, 251, 0.21);
}
.list-title { margin: 0 0 32px 0; font-size: 22px; text-align: center; color: rgb(128, 160, 152); font-weight: 600; letter-spacing: 2px; }
.contact-items { flex: 1; overflow-y: auto; }
.contact-item { padding: 16px 0 12px 0; position: relative; margin-bottom: 4px; cursor: pointer; display: flex; align-items: center; transition: opacity 0.2s; }
.contact-item:hover { opacity: 0.8; }
.contact-item.active .contact-name { font-weight: 700; }
.contact-item::after {
  content: ''; position: absolute; bottom: 0; left: 0; width: 100%; height: 1px;
  background: linear-gradient(to right, rgba(128, 160, 152, 0), rgba(128, 160, 152, 1) 30%, rgba(128, 160, 152, 1) 70%, rgba(128, 160, 152, 0));
}
.contact-avatar-wrapper { position: relative; width: 40px; height: 40px; flex-shrink: 0; margin-right: 12px; }
.contact-avatar { width: 100%; height: 100%; border-radius: 50%; background: rgba(128, 160, 152, 0.2); color: rgb(128, 160, 152); display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600; overflow: hidden; }
.contact-avatar.group-avatar { background: rgba(128, 160, 152, 0.1); font-size: 20px; }
.contact-avatar img { width: 100%; height: 100%; object-fit: cover; }
.unread-badge { position: absolute; top: -4px; right: -4px; background: #ff4d4f; color: white; border-radius: 50%; min-width: 18px; height: 18px; font-size: 11px; display: flex; align-items: center; justify-content: center; padding: 0 4px; font-weight: 600; border: 2px solid rgba(150, 255, 251, 0.21); }
.contact-info { flex: 1; min-width: 0; }
.contact-name { font-size: 16px; font-weight: 500; color: rgb(128, 160, 152); margin-bottom: 4px; }
.contact-msg { font-size: 13px; color: rgba(128, 160, 152, 0.6); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.contact-time { font-size: 12px; color: rgba(128, 160, 152, 0.5); white-space: nowrap; flex-shrink: 0; margin-left: 8px; }
.status-text { text-align: center; color: rgba(128, 160, 152, 0.8); padding: 40px 0; font-size: 15px; }

/* 右侧聊天框 */
.chat-area-wrapper { flex: 1; height: 100%; display: flex; flex-direction: column; padding: 40px 40px 0 30px; box-sizing: border-box; }
.chat-header { padding-bottom: 20px; flex-shrink: 0; }
.chat-title { color: rgb(128, 160, 152); font-size: 18px; font-weight: 600; }

.message-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; padding-right: 16px; margin-bottom: 16px; }
.message-item { display: flex; gap: 12px; max-width: 80%; align-items: flex-end; }
.message-item.message-self { align-self: flex-end; flex-direction: row-reverse; }

/* ✅ 修复：聊天框内的消息头像样式 */
.message-avatar-wrapper { flex-shrink: 0; }
.message-avatar { width: 34px; height: 34px; border-radius: 50%; background: rgba(128, 160, 152, 0.2); color: rgb(128, 160, 152); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; overflow: hidden; }
.message-avatar img { width: 100%; height: 100%; object-fit: cover; }

.message-body { display: flex; flex-direction: column; gap: 2px; }
.message-bubble { background: rgba(245, 255, 253, 0.80); padding: 12px 16px; border-radius: 16px; max-width: 100%; word-break: break-word; color: #2c3e32; box-shadow: 0 2px 8px rgba(0,0,0,0.02); }
.message-self .message-bubble { background: rgba(128, 160, 152, 0.15); }
.message-text { font-size: 14px; line-height: 1.5; }
.message-time { font-size: 11px; color: rgba(44, 62, 50, 0.4); text-align: right; margin-top: 6px; }
.message-image img { max-width: 200px; max-height: 200px; border-radius: 8px; cursor: pointer; }
.message-file { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #2c3e32; text-decoration: underline; }

.input-area { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; padding-top: 12px; background: rgba(245, 255, 253, 0.80); border-top-left-radius: 20px; border-top: 1px solid rgba(128, 160, 152, 0.1); padding: 16px 20px 24px 20px; margin-left: -30px; margin-right: -40px; padding-right: 40px; }
.input-tools { display: flex; gap: 12px; }
.tool-btn { background: none; border: none; font-size: 18px; cursor: pointer; color: rgb(128, 160, 152); padding: 0; }
.input-row { display: flex; gap: 12px; }
.input-row input { flex: 1; padding: 10px 16px; border: 1px solid rgba(128, 160, 152, 0.25); background: rgba(255, 255, 255, 0.5); border-radius: 20px; font-size: 14px; outline: none; color: #2c3e32; }
.input-row input:focus { border-color: rgba(128, 160, 152, 0.7); }
.send-btn { padding: 0 32px; height: 40px; border: none; border-radius: 40px; background: rgb(128, 160, 152); color: #fff; font-size: 14px; font-weight: 500; cursor: pointer; transition: opacity 0.2s; }
.send-btn:hover { opacity: 0.85; }

.file-preview { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }
.file-preview-item { display: flex; align-items: center; gap: 6px; background: rgba(255, 255, 255, 0.6); padding: 4px 12px; border-radius: 12px; font-size: 13px; color: #2c3e32; }
.file-preview-item button { background: none; border: none; cursor: pointer; color: #999; }
</style>