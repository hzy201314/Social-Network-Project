<template>
  <div class="chat-container">
    <!-- 左侧联系人列表 -->
    <div class="contact-list">
      <div class="contact-header">
        <h2>💬 消息</h2>
        <button @click="showCreateGroup = true" class="create-btn" title="创建群聊">+</button>
      </div>
      <div class="contact-search">
        <input type="text" placeholder="搜索" v-model="searchKeyword" />
      </div>
      <div class="contact-items">
        <div 
          v-for="item in filteredContacts" 
          :key="item.id"
          class="contact-item"
          :class="{ active: currentContactId === item.id }"
          @click="selectContact(item.id)"
        >
          <div class="contact-avatar">
            <template v-if="item.type === 'group'">
              <img v-if="item.avatar" :src="item.avatar" alt="群头像" class="group-avatar-img" />
              <span v-else class="group-avatar">👥</span>
            </template>
            <template v-else>
              <img 
                v-if="getUserAvatar(item.id, item.avatar, item.username)" 
                :src="getUserAvatar(item.id, item.avatar, item.username)" 
                alt="头像" 
              />
              <span v-else>{{ (item.nickname || item.username || '?').charAt(0) }}</span>
            </template>
            <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount }}</span>
          </div>
          <div class="contact-info">
            <div class="contact-name">{{ item.type === 'group' ? item.name : (item.nickname || item.username) }}</div>
            <div class="contact-last-msg">{{ item.lastMessage || '' }}</div>
          </div>
          <div class="contact-time">{{ item.lastTime || '' }}</div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <div class="chat-header">
        <div class="chat-title">{{ currentContact?.type === 'group' ? currentContact?.name : (currentContact?.nickname || currentContact?.username || '选择联系人') }}</div>
        <div class="chat-actions">
          <button @click="openContactInfo" class="info-btn">ℹ️</button>
        </div>
      </div>

      <div class="message-list" ref="messageListRef">
        <div v-if="loading" class="loading-text">加载中...</div>
        <div v-else-if="messages.length === 0" class="empty-text">暂无消息，打个招呼吧！</div>
        <div 
          v-else 
          v-for="msg in messages" 
          :key="msg.id"
          class="message-item"
          :class="{ 'message-self': msg.senderId === currentUserId }"
        >
          <div class="message-avatar">
            <img 
              v-if="getUserAvatar(msg.senderId, msg.senderAvatar, msg.senderUsername)" 
              :src="getUserAvatar(msg.senderId, msg.senderAvatar, msg.senderUsername)" 
              alt="头像" 
            />
            <span v-else>{{ (msg.senderNickname || msg.senderUsername || '?').charAt(0) }}</span>
          </div>
          <div class="message-body">
            <div class="message-sender">{{ getUserNickname(msg.senderId, msg.senderNickname, msg.senderUsername) }}</div>
            <div class="message-bubble">
              <div v-if="msg.type === 'text' || !msg.type" class="message-text" v-html="formatMessageContent(msg.content)">
              </div>
              <div v-else-if="msg.type === 'image'" class="message-image">
                <img :src="msg.content" @click="previewImage(msg.content)" alt="图片" />
              </div>
              <div v-else-if="msg.fileType || msg.type === 'file' || msg.fileUrl" class="message-file" @click="handleFileClick(msg)">
                <span>📎</span>
                <span>{{ msg.fileName || '文件' }}</span>
                <span class="file-size" v-if="msg.fileSize">{{ formatFileSize(msg.fileSize) }}</span>
              </div>
              <div v-else-if="msg.type === 'video'" class="message-video">
                <video :src="msg.content" controls></video>
              </div>
              <div v-else class="message-text" v-html="formatMessageContent(msg.content)">
              </div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

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
            ref="messageInput"
          />
          <button @click="sendMessage" class="send-btn">发送</button>
        </div>
        <div v-if="pendingFiles.length > 0" class="file-preview">
          <div v-for="(file, index) in pendingFiles" :key="index" class="file-preview-item">
            <span>{{ file.name }}</span>
            <button @click="removePendingFile(index)">✕</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 创建群聊弹窗 ===== -->
    <div v-if="showCreateGroup" class="modal-overlay" @click.self="showCreateGroup = false">
      <div class="modal">
        <h2>创建群聊</h2>
        <div class="form-group">
          <label>群名称</label>
          <input v-model="newGroupName" placeholder="请输入群名称" />
        </div>
        <div class="form-group">
          <label>邀请好友</label>
          <div class="friend-select">
            <div 
              v-for="friend in friends" 
              :key="friend.id"
              class="friend-option"
              :class="{ selected: selectedFriends.includes(friend.id) }"
              @click="toggleFriend(friend.id)"
            >
              {{ friend.nickname || friend.username }}
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="showCreateGroup = false" class="cancel-btn">取消</button>
          <button @click="createGroup" class="save-btn">创建</button>
        </div>
      </div>
    </div>

    <!-- ===== 联系人/群信息弹窗 ===== -->
    <div v-if="showContactInfo" class="modal-overlay" @click.self="showContactInfo = false">
      <div class="modal">
        <h2>{{ currentContact?.type === 'group' ? '群信息' : '好友信息' }}</h2>
        
        <div v-if="currentContact?.type === 'group'" class="group-avatar-edit">
          <div class="group-avatar-preview" @click="triggerGroupAvatarUpload">
            <img v-if="currentContact?.avatar" :src="currentContact.avatar" alt="群头像" />
            <span v-else>👥</span>
            <div class="avatar-edit-overlay">📷</div>
          </div>
          <input type="file" ref="groupAvatarInput" accept="image/*" style="display:none" @change="uploadGroupAvatar" />
        </div>
        
        <div class="group-info-item">
          <strong>名称：</strong>
          <span v-if="!isEditingGroup">{{ currentContact?.name }}</span>
          <input v-else v-model="editGroupName" placeholder="请输入新群名称" />
        </div>
        <div v-if="currentContact?.type === 'group'">
          <button v-if="!isEditingGroup" @click="startEditGroup" class="edit-btn">✏️ 修改名称</button>
          <button v-else @click="saveGroupName" class="save-btn">保存</button>
          <button v-if="isEditingGroup" @click="isEditingGroup = false" class="cancel-btn">取消</button>
        </div>

        <div v-if="currentContact?.type === 'group'" class="group-info-item">
          <strong>公告：</strong>
          <span v-if="!isEditingAnnouncement">{{ currentContact?.announcement || '暂无公告' }}</span>
          <input v-else v-model="editAnnouncement" placeholder="请输入群公告" />
        </div>
        <div v-if="currentContact?.type === 'group'">
          <button v-if="!isEditingAnnouncement" @click="startEditAnnouncement" class="edit-btn">✏️ 修改公告</button>
          <button v-else @click="saveAnnouncement" class="save-btn">保存</button>
          <button v-if="isEditingAnnouncement" @click="isEditingAnnouncement = false" class="cancel-btn">取消</button>
        </div>
        
        <div v-if="currentContact?.type === 'group'" class="group-info-item">
          <strong>群主：</strong>
          <span>{{ currentContact?.ownerNickname || currentContact?.ownerUsername || '未知' }}</span>
        </div>
        
        <div v-if="currentContact?.type === 'group'" class="group-info-item">
          <strong>成员（{{ groupMembers.length }}人）：</strong>
        </div>
        <div v-if="currentContact?.type === 'group'" class="member-list">
          <div v-for="member in groupMembers" :key="member.userId || member.id" class="member-item">
            {{ member.nickname || member.username || '用户' }}
            <span v-if="member.userId === currentContact?.ownerId || member.id === currentContact?.ownerId" class="owner-tag">群主</span>
          </div>
        </div>
        
        <div class="modal-actions">
          <button v-if="currentContact?.type === 'group'" @click="leaveGroup" class="cancel-btn" v-show="!isGroupOwner">退出群聊</button>
          <button v-if="currentContact?.type === 'group'" @click="dissolveGroup" class="danger-btn" v-show="isGroupOwner">解散群聊</button>
          <button @click="showContactInfo = false" class="save-btn">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="previewImageUrl" class="modal-overlay" @click="previewImageUrl = null">
      <div class="modal-content">
        <img :src="previewImageUrl" alt="预览" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { uploadFile } from '@/utils/upload'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

const router = useRouter()
const currentUserId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)

// ===== 联系人列表 =====
const contacts = ref([])
const currentContactId = ref(null)
const searchKeyword = ref('')
const loading = ref(false)

// ===== 消息 =====
const messages = ref([])
const inputMessage = ref('')
const messageListRef = ref(null)
const messageInput = ref(null)

// ===== 文件 =====
const pendingFiles = ref([])
const previewImageUrl = ref(null)

// ===== 弹窗 =====
const showCreateGroup = ref(false)
const showContactInfo = ref(false)
const newGroupName = ref('')
const selectedFriends = ref([])
const friends = ref([])
const groupMembers = ref([])

// ===== 群名称修改 =====
const isEditingGroup = ref(false)
const editGroupName = ref('')
const groupAvatarInput = ref(null)

// ===== 群公告修改 =====
const isEditingAnnouncement = ref(false)
const editAnnouncement = ref('')

let pollingTimer = null

// ===== 计算属性 =====
const filteredContacts = computed(() => {
  if (!searchKeyword.value) return contacts.value
  const kw = searchKeyword.value.toLowerCase()
  return contacts.value.filter(item => {
    const name = item.type === 'group' ? item.name : (item.nickname || item.username)
    return name.toLowerCase().includes(kw)
  })
})

const currentContact = computed(() => {
  return contacts.value.find(c => c.id === currentContactId.value)
})

const isGroupOwner = computed(() => {
  return currentContact.value?.type === 'group' && currentContact.value?.ownerId === currentUserId.value
})

// ============================================================
// ✅ 核心方法：格式化消息内容，将 URL 转换为可点击链接
// ============================================================
const formatMessageContent = (content) => {
  if (!content) return ''
  
  if (content.match(/\/uploads\//) && !content.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i)) {
    return content
  }
  
  const urlRegex = /(https?:\/\/[^\s]+)/g
  
  let result = content
  const urls = content.match(urlRegex)
  
  if (urls) {
    urls.forEach(url => {
      if (url.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i)) {
        const imgHtml = `<img src="${url}" style="max-width:180px;max-height:180px;border-radius:8px;cursor:pointer;display:block;margin:4px 0;" onclick="window.open('${url}','_blank')" />`
        result = result.replace(url, imgHtml)
      } else {
        const linkHtml = `<a href="${url}" target="_blank" style="color:#667eea;text-decoration:underline;word-break:break-all;" onclick="event.stopPropagation();">${url}</a>`
        result = result.replace(url, linkHtml)
      }
    })
  }
  
  result = result.replace(/\n/g, '<br />')
  
  return result
}

// ===== 获取消息摘要（用于列表显示） =====
const getMessageSummary = (msg) => {
  if (!msg) return ''
  // 如果有文件
  if (msg.fileUrl) {
    if (msg.fileType === 'image') return '📷 [图片]'
    if (msg.fileType === 'video') return '🎬 [视频]'
    return `📎 ${msg.fileName || '文件'}`
  }
  // 纯文本
  return msg.content || ''
}

// ===== 格式化时间显示（列表用） =====
const formatListTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 172800) return '昨天'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  
  // 超过一周显示日期
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

// ===== ✅ 更新联系人最后消息（基于最新消息） =====
const updateContactLastMessage = (contactId, type, latestMsg) => {
  const contact = contacts.value.find(c => c.id === contactId)
  if (!contact) return
  
  contact.lastMessage = getMessageSummary(latestMsg)
  contact.lastTime = formatListTime(latestMsg.createdAt)
  contact._lastMsgTime = new Date(latestMsg.createdAt).getTime()
  
  // ✅ 保存到 localStorage
  const key = type === 'group' ? `group_${contactId}` : `friend_${contactId}`
  try {
    const saved = localStorage.getItem('chatLastMessages')
    const parsed = saved ? JSON.parse(saved) : {}
    parsed[key] = {
      lastMessage: contact.lastMessage,
      lastTime: contact.lastTime,
      _lastMsgTime: contact._lastMsgTime
    }
    localStorage.setItem('chatLastMessages', JSON.stringify(parsed))
  } catch (e) {}
  
  // ✅ 重新排序
  sortContacts()
}

// ===== ✅ 排序联系人（按最后消息时间倒序） =====
const sortContacts = () => {
  contacts.value.sort((a, b) => {
    const timeA = a._lastMsgTime || 0
    const timeB = b._lastMsgTime || 0
    return timeB - timeA
  })
}

// ===== 加载联系人（修复版） =====
const loadContacts = async () => {
  try {
    const friendRes = await request.get('/api/friends')
    const friendList = friendRes.data.code === 0 ? (friendRes.data.data || []) : []
    
    const groupRes = await request.get('/api/groups')
    const groupList = groupRes.data.code === 0 ? (groupRes.data.data || []) : []
    
    const allContacts = [
      ...friendList.map(f => ({
        ...f,
        type: 'friend',
        unreadCount: 0,
        lastMessage: '',
        lastTime: '',
        _lastMsgTime: 0
      })),
      ...groupList.map(g => ({
        ...g,
        type: 'group',
        unreadCount: 0,
        lastMessage: '',
        lastTime: '',
        _lastMsgTime: 0
      }))
    ]
    
    // ✅ 先从 localStorage 恢复缓存数据（立即显示）
    try {
      const saved = localStorage.getItem('chatLastMessages')
      if (saved) {
        const parsed = JSON.parse(saved)
        allContacts.forEach(c => {
          const key = c.type === 'group' ? `group_${c.id}` : `friend_${c.id}`
          if (parsed[key]) {
            c.lastMessage = parsed[key].lastMessage || ''
            c.lastTime = parsed[key].lastTime || ''
            c._lastMsgTime = parsed[key]._lastMsgTime || 0
          }
        })
      }
    } catch (e) {}
    
    contacts.value = allContacts
    
    // ✅ 先排序一次（用缓存数据）
    sortContacts()
    
    // ✅ 后台异步加载最新消息（刷新数据）
    await loadLatestMessagesForAllContacts()
    
    // ✅ 加载完成后再次排序
    sortContacts()
    
    // ✅ 默认选中第一个联系人
    if (contacts.value.length > 0 && !currentContactId.value) {
      currentContactId.value = contacts.value[0].id
      const contact = contacts.value.find(c => c.id === currentContactId.value)
      if (contact) {
        loadMessages(contact.id, contact.type)
      }
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
  }
}

// ===== ✅ 加载所有联系人的最新消息（修复版） =====
const loadLatestMessagesForAllContacts = async () => {
  // 使用 Promise.all 并发请求，提升加载速度
  const promises = contacts.value.map(async (contact) => {
    try {
      let res
      if (contact.type === 'group') {
        // ✅ 群聊接口可能不支持 size 参数，先获取全部然后取最后一条
        res = await request.get(`/api/groups/${contact.id}/messages`)
      } else {
        // ✅ 私聊接口，获取最新一条
        res = await request.get(`/api/messages/${contact.id}`)
      }
      
      if (res.data.code === 0 && res.data.data && res.data.data.length > 0) {
        // ✅ 取最后一条（时间最新的）
        const allMessages = res.data.data
        const latestMsg = allMessages[allMessages.length - 1]
        
        // ✅ 直接更新 contact 对象
        contact.lastMessage = getMessageSummary(latestMsg)
        contact.lastTime = formatListTime(latestMsg.createdAt)
        contact._lastMsgTime = new Date(latestMsg.createdAt).getTime()
        
        // ✅ 同时保存到 localStorage
        const key = contact.type === 'group' ? `group_${contact.id}` : `friend_${contact.id}`
        const saved = localStorage.getItem('chatLastMessages')
        const parsed = saved ? JSON.parse(saved) : {}
        parsed[key] = {
          lastMessage: contact.lastMessage,
          lastTime: contact.lastTime,
          _lastMsgTime: contact._lastMsgTime
        }
        localStorage.setItem('chatLastMessages', JSON.stringify(parsed))
      }
    } catch (error) {
      // 单个联系人加载失败不影响整体
      console.debug('加载联系人最新消息失败:', contact.id, error)
    }
  })
  
  // ✅ 等待所有请求完成
  await Promise.all(promises)
}


// ===== 保存最后消息到 localStorage =====
const saveLastMessageToStorage = (contactId, type, message) => {
  try {
    const key = type === 'group' ? `group_${contactId}` : `friend_${contactId}`
    const saved = localStorage.getItem('chatLastMessages')
    const parsed = saved ? JSON.parse(saved) : {}
    parsed[key] = {
      lastMessage: getMessageSummary(message),
      lastTime: formatListTime(message.createdAt),
      _lastMsgTime: new Date(message.createdAt).getTime()
    }
    localStorage.setItem('chatLastMessages', JSON.stringify(parsed))
  } catch (e) {}
}

// ===== 加载消息 =====
const loadMessages = async (contactId, type) => {
  if (!contactId) return
  loading.value = true
  try {
    let res
    if (type === 'group') {
      res = await request.get(`/api/groups/${contactId}/messages`)
    } else {
      res = await request.get(`/api/messages/${contactId}`)
    }
    if (res.data.code === 0) {
      const rawMessages = res.data.data || []
      messages.value = rawMessages.map(msg => {
        const isFileUrl = msg.content && (
          msg.content.startsWith('/uploads/') ||
          msg.content.startsWith('http') && (
            msg.content.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp|mp4|webm|ogg|mov|avi|pdf|doc|docx|xls|xlsx|txt)$/i)
          )
        )
        
        return {
          ...msg,
          fileType: msg.fileType || msg.type || (isFileUrl ? 'file' : null),
          fileName: msg.fileName || (isFileUrl ? msg.content.split('/').pop() || '文件' : null),
          fileUrl: msg.fileUrl || (isFileUrl ? msg.content : null),
          type: msg.type || (isFileUrl ? 'file' : 'text')
        }
      })
      
      // ✅ 如果有消息，更新联系人最后消息
      if (messages.value.length > 0) {
        const latest = messages.value[messages.value.length - 1]
        updateContactLastMessage(contactId, type, latest)
        saveLastMessageToStorage(contactId, type, latest)
      }
      
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loading.value = false
  }
}

// ===== 选择联系人 =====
const selectContact = (contactId) => {
  if (currentContactId.value === contactId) return
  currentContactId.value = contactId
  const contact = contacts.value.find(c => c.id === contactId)
  loadMessages(contactId, contact?.type)
  messageInput.value?.focus()
}

// ===== 发送消息 =====
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content && pendingFiles.value.length === 0) return

  const contact = currentContact.value
  if (!contact) {
    alert('请先选择一个联系人')
    return
  }

  let uploadedFile = null
  if (pendingFiles.value.length > 0) {
    const file = pendingFiles.value[0]
    const url = await uploadFile(file)
    if (url) {
      let fileType = 'file'
      if (file.type.startsWith('image/')) fileType = 'image'
      else if (file.type.startsWith('video/')) fileType = 'video'
      else if (file.type === 'application/pdf') fileType = 'pdf'
      
      uploadedFile = {
        url: url,
        name: file.name,
        type: fileType,
        size: file.size,
        mimeType: file.type
      }
    }
    pendingFiles.value = []
  }

  try {
    let res
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    
    const messageBody = {
      receiverId: contact.id,
      content: content || (uploadedFile ? uploadedFile.url : ''),
      fileUrl: uploadedFile ? uploadedFile.url : null,
      fileType: uploadedFile ? uploadedFile.type : null,
      fileName: uploadedFile ? uploadedFile.name : null,
      fileSize: uploadedFile ? uploadedFile.size : 0
    }

    if (contact.type === 'group') {
      const groupMessageBody = {
        content: content || (uploadedFile ? uploadedFile.url : ''),
        fileUrl: uploadedFile ? uploadedFile.url : null,
        fileType: uploadedFile ? uploadedFile.type : null,
        fileName: uploadedFile ? uploadedFile.name : null,
        fileSize: uploadedFile ? uploadedFile.size : 0
      }
      res = await request.post(`/api/groups/${contact.id}/messages`, groupMessageBody)
    } else {
      res = await request.post('/api/messages/send', messageBody)
    }
    
    if (res.data.code === 0) {
      inputMessage.value = ''
      
      const newMsg = {
        ...res.data.data,
        id: res.data.data?.id || Date.now(),
        senderId: currentUserId.value,
        senderAvatar: user.avatar,
        senderNickname: user.nickname || user.username,
        senderUsername: user.username,
        fileUrl: uploadedFile ? uploadedFile.url : (res.data.data?.fileUrl || null),
        fileType: uploadedFile ? uploadedFile.type : (res.data.data?.fileType || null),
        fileName: uploadedFile ? uploadedFile.name : (res.data.data?.fileName || null),
        fileSize: uploadedFile ? uploadedFile.size : (res.data.data?.fileSize || 0),
        type: uploadedFile ? uploadedFile.type : (res.data.data?.type || 'text'),
        content: content || (uploadedFile ? uploadedFile.url : ''),
        createdAt: new Date().toISOString()
      }
      messages.value.push(newMsg)
      await scrollToBottom()
      
      // ✅ 更新联系人最后消息
      updateContactLastMessage(contact.id, contact.type, newMsg)
      saveLastMessageToStorage(contact.id, contact.type, newMsg)
    } else {
      alert(res.data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送失败:', error)
    alert('发送失败，请重试')
  }
}

// ===== 滚动到底部 =====
const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// ===== 文件上传 =====
const triggerFileUpload = (type) => {
  const input = document.createElement('input')
  input.type = 'file'
  if (type === 'image') {
    input.accept = 'image/*'
  } else if (type === 'video') {
    input.accept = 'video/*'
  } else {
    input.accept = '.pdf,.doc,.docx,.xls,.xlsx,.txt'
  }
  input.multiple = true
  input.onchange = (e) => {
    const files = Array.from(e.target.files)
    pendingFiles.value.push(...files)
  }
  input.click()
}

const removePendingFile = (index) => {
  pendingFiles.value.splice(index, 1)
}

const downloadFile = (url, fileName) => {
  const a = document.createElement('a')
  a.href = url
  a.download = fileName || 'file'
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

const previewImage = (url) => {
  previewImageUrl.value = url
}

// ===== 处理文件点击 =====
const handleFileClick = (msg) => {
  const url = msg.fileUrl || msg.content
  if (!url) {
    alert('文件链接无效')
    return
  }
  
  const fileType = msg.fileType || msg.type || 'file'
  const fileName = msg.fileName || '文件'
  
  console.log('📎 点击文件:', { url, fileType, fileName })
  
  if (fileType === 'image' || url.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i)) {
    previewImage(url)
    return
  }
  
  if (fileType === 'pdf' || url.match(/\.pdf$/i)) {
    window.open(url, '_blank')
    return
  }
  
  if (fileType === 'video' || url.match(/\.(mp4|webm|ogg|mov|avi)$/i)) {
    window.open(url, '_blank')
    return
  }
  
  downloadFile(url, fileName)
}

// ===== ✅ 轮询所有联系人是否有新消息 =====
const pollAllNewMessages = async () => {
  try {
    // 遍历所有联系人
    for (const contact of contacts.value) {
      try {
        let res
        if (contact.type === 'group') {
          res = await request.get(`/api/groups/${contact.id}/messages`)
        } else {
          res = await request.get(`/api/messages/${contact.id}`)
        }
        
        if (res.data.code === 0 && res.data.data && res.data.data.length > 0) {
          const rawMessages = res.data.data || []
          const latestMsg = rawMessages[rawMessages.length - 1]
          
          // ✅ 检查这个联系人是否在 contacts 列表中有记录
          const contactInList = contacts.value.find(c => c.id === contact.id)
          if (!contactInList) continue
          
          // ✅ 检查是否有新消息（比较最后消息的时间或ID）
          const newLastMsgTime = new Date(latestMsg.createdAt).getTime()
          if (newLastMsgTime > (contactInList._lastMsgTime || 0)) {
            // ✅ 更新联系人的最后消息
            contactInList.lastMessage = getMessageSummary(latestMsg)
            contactInList.lastTime = formatListTime(latestMsg.createdAt)
            contactInList._lastMsgTime = newLastMsgTime
            
            // ✅ 保存到 localStorage
            const key = contact.type === 'group' ? `group_${contact.id}` : `friend_${contact.id}`
            try {
              const saved = localStorage.getItem('chatLastMessages')
              const parsed = saved ? JSON.parse(saved) : {}
              parsed[key] = {
                lastMessage: contactInList.lastMessage,
                lastTime: contactInList.lastTime,
                _lastMsgTime: contactInList._lastMsgTime
              }
              localStorage.setItem('chatLastMessages', JSON.stringify(parsed))
            } catch (e) {}
            
            // ✅ 如果当前选中的是这个联系人，也更新消息列表
            if (currentContactId.value === contact.id) {
              const currentMsgRes = await request.get(
                contact.type === 'group' 
                  ? `/api/groups/${contact.id}/messages` 
                  : `/api/messages/${contact.id}`
              )
              if (currentMsgRes.data.code === 0) {
                const newMessages = currentMsgRes.data.data || []
                messages.value = newMessages.map(msg => {
                  const isFileUrl = msg.content && (
                    msg.content.startsWith('/uploads/') ||
                    msg.content.startsWith('http') && (
                      msg.content.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp|mp4|webm|ogg|mov|avi|pdf|doc|docx|xls|xlsx|txt)$/i)
                    )
                  )
                  return {
                    ...msg,
                    fileType: msg.fileType || msg.type || (isFileUrl ? 'file' : null),
                    fileName: msg.fileName || (isFileUrl ? msg.content.split('/').pop() || '文件' : null),
                    fileUrl: msg.fileUrl || (isFileUrl ? msg.content : null),
                    type: msg.type || (isFileUrl ? 'file' : 'text')
                  }
                })
                await scrollToBottom()
              }
            }
          }
        }
      } catch (error) {
        // 单个联系人加载失败不影响整体
        console.debug('轮询联系人失败:', contact.id, error)
      }
    }
    
    // ✅ 全部轮询完成后重新排序
    sortContacts()
    
  } catch (error) {
    console.error('轮询所有消息失败:', error)
  }
}

// ===== 修改生命周期 =====
onMounted(async () => {
  await loadContacts()
  try {
    const res = await request.get('/api/friends')
    if (res.data.code === 0) {
      friends.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载好友失败:', error)
  }
  // ✅ 改成轮询所有联系人
  pollingTimer = setInterval(pollAllNewMessages, 3000)
})

// ===== 创建群聊 =====
const createGroup = async () => {
  if (!newGroupName.value.trim()) {
    alert('请输入群名称')
    return
  }

  try {
    const res = await request.post('/api/groups', {
      name: newGroupName.value,
      memberIds: selectedFriends.value
    })
    if (res.data.code === 0) {
      alert('群聊创建成功！')
      showCreateGroup.value = false
      newGroupName.value = ''
      selectedFriends.value = []
      await loadContacts()
    } else {
      alert(res.data.message || '创建失败')
    }
  } catch (error) {
    console.error('创建群聊失败:', error)
    alert('创建失败，请重试')
  }
}

const toggleFriend = (friendId) => {
  const index = selectedFriends.value.indexOf(friendId)
  if (index > -1) {
    selectedFriends.value.splice(index, 1)
  } else {
    selectedFriends.value.push(friendId)
  }
}

// ===== 加载群成员 =====
const loadGroupMembers = async () => {
  if (!currentContactId.value || currentContact.value?.type !== 'group') return
  try {
    const res = await request.get(`/api/groups/${currentContactId.value}/members`)
    if (res.data.code === 0) {
      groupMembers.value = (res.data.data || []).map(m => ({
        userId: m.userId || m.id,
        id: m.id || m.userId,
        username: m.username || '',
        nickname: m.nickname || m.username || '用户',
        avatar: m.avatar || null,
        role: m.role || 0
      }))
    }
  } catch (error) {
    console.error('加载群成员失败:', error)
    groupMembers.value = []
  }
}

// ===== 群名称修改 =====
const startEditGroup = () => {
  editGroupName.value = currentContact.value?.name || ''
  isEditingGroup.value = true
}

const saveGroupName = async () => {
  if (!editGroupName.value.trim()) {
    alert('请输入群名称')
    return
  }
  try {
    const res = await request.put(`/api/groups/${currentContactId.value}`, {
      name: editGroupName.value.trim()
    })
    if (res.data.code === 0) {
      alert('群名称修改成功！')
      const contact = contacts.value.find(c => c.id === currentContactId.value)
      if (contact) {
        contact.name = editGroupName.value.trim()
      }
      isEditingGroup.value = false
      await loadContacts()
    } else {
      alert(res.data.message || '修改失败')
    }
  } catch (error) {
    console.error('修改群名称失败:', error)
    alert('修改失败，请重试')
  }
}

// ===== 群头像上传 =====
const triggerGroupAvatarUpload = () => {
  groupAvatarInput.value?.click()
}

const uploadGroupAvatar = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    alert('请上传图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    alert('图片大小不能超过2MB')
    return
  }
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadRes = await request.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (uploadRes.data.code !== 0) {
      alert('头像上传失败')
      return
    }
    const avatarUrl = uploadRes.data.data
    
    const res = await request.put(`/api/groups/${currentContactId.value}`, {
      avatar: avatarUrl
    })
    if (res.data.code === 0) {
      alert('群头像更新成功！')
      const contact = contacts.value.find(c => c.id === currentContactId.value)
      if (contact) {
        contact.avatar = avatarUrl
      }
      await loadContacts()
      if (currentContactId.value) {
        const refreshedContact = contacts.value.find(c => c.id === currentContactId.value)
        if (refreshedContact) {
          loadMessages(refreshedContact.id, refreshedContact.type)
        }
      }
    } else {
      alert(res.data.message || '更新失败')
    }
  } catch (error) {
    console.error('上传群头像失败:', error)
    alert('上传失败，请重试')
  }
}

// ===== 群公告修改 =====
const startEditAnnouncement = () => {
  editAnnouncement.value = currentContact.value?.announcement || ''
  isEditingAnnouncement.value = true
}

const saveAnnouncement = async () => {
  try {
    const res = await request.put(`/api/groups/${currentContactId.value}`, {
      announcement: editAnnouncement.value.trim()
    })
    if (res.data.code === 0) {
      alert('群公告修改成功！')
      const contact = contacts.value.find(c => c.id === currentContactId.value)
      if (contact) {
        contact.announcement = editAnnouncement.value.trim()
      }
      isEditingAnnouncement.value = false
      await loadContacts()
    } else {
      alert(res.data.message || '修改失败')
    }
  } catch (error) {
    console.error('修改群公告失败:', error)
    alert('修改失败，请重试')
  }
}

// ===== 退出/解散群聊 =====
const leaveGroup = async () => {
  if (!confirm('确定要退出这个群聊吗？')) return
  try {
    const res = await request.delete(`/api/groups/${currentContactId.value}/leave`)
    if (res.data.code === 0) {
      alert('已退出群聊')
      showContactInfo.value = false
      await loadContacts()
    } else {
      alert(res.data.message || '退出失败')
    }
  } catch (error) {
    console.error('退出群聊失败:', error)
    alert('退出失败，请重试')
  }
}

const dissolveGroup = async () => {
  if (!confirm('确定要解散这个群聊吗？此操作不可撤销！')) return
  try {
    const res = await request.delete(`/api/groups/${currentContactId.value}`)
    if (res.data.code === 0) {
      alert('群聊已解散')
      showContactInfo.value = false
      await loadContacts()
    } else {
      alert(res.data.message || '解散失败')
    }
  } catch (error) {
    console.error('解散群聊失败:', error)
    alert('解散失败，请重试')
  }
}

// ===== 格式化时间 =====
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// ===== 格式化文件大小 =====
const formatFileSize = (bytes) => {
  if (!bytes) return ''
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i]
}

// ===== 打开群信息 =====
const openContactInfo = async () => {
  showContactInfo.value = true
  if (currentContact.value?.type === 'group') {
    await loadGroupMembers()
  }
}

// ===== 生命周期 =====
onMounted(async () => {
  await loadContacts()
  try {
    const res = await request.get('/api/friends')
    if (res.data.code === 0) {
      friends.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载好友失败:', error)
  }
  pollingTimer = setInterval(pollNewMessages, 1000)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  max-width: 1000px;
  margin: 0 auto;
  background: #f5f7fa;
  overflow: hidden;
}

.contact-list {
  width: 280px;
  background: white;
  border-right: 1px solid #e8ecf1;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.contact-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 8px 16px;
}
.contact-header h2 { font-size: 18px; margin: 0; }
.create-btn {
  background: #667eea;
  color: white;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 20px;
  cursor: pointer;
}

.contact-search { padding: 8px 16px; }
.contact-search input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.contact-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}
.contact-item:hover { background: #f0f2f5; }
.contact-item.active { background: #e8ecf1; }

.contact-avatar {
  position: relative;
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
.contact-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.group-avatar { font-size: 20px; }
.group-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff4d4f;
  color: white;
  border-radius: 50%;
  min-width: 20px;
  height: 20px;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 600;
}

.contact-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}
.contact-name { font-weight: 600; font-size: 15px; }
.contact-last-msg {
  font-size: 13px;
  color: #9aa6b5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.contact-time {
  font-size: 11px;
  color: #9aa6b5;
  flex-shrink: 0;
  margin-left: 8px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #e8ecf1;
  flex-shrink: 0;
}
.chat-title { font-size: 16px; font-weight: 600; }
.info-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  padding: 4px 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.loading-text, .empty-text {
  text-align: center;
  color: #9aa6b5;
  padding: 40px 0;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 80%;
}
.message-item.message-self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
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
  flex-shrink: 0;
  overflow: hidden;
}
.message-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.message-sender {
  font-size: 12px;
  color: #9aa6b5;
  margin-left: 4px;
}
.message-self .message-sender { text-align: right; margin-right: 4px; }

.message-bubble {
  background: white;
  padding: 10px 14px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  max-width: 100%;
  word-break: break-word;
}
.message-self .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
.message-self .message-bubble a {
  color: #ffffff !important;
}
.message-text { font-size: 14px; word-wrap: break-word; }
.message-text a { color: #667eea; text-decoration: underline; }
.message-self .message-text a { color: #ffffff !important; }
.message-time {
  font-size: 11px;
  color: #9aa6b5;
  text-align: right;
  margin-top: 4px;
}
.message-self .message-time { color: rgba(255, 255, 255, 0.7); }

.message-image img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
}
.message-video video {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
}
.message-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f2f5;
  border-radius: 8px;
  cursor: pointer;
}
.message-self .message-file { background: rgba(255, 255, 255, 0.2); }

.input-area {
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #e8ecf1;
  flex-shrink: 0;
}
.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.tool-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
  padding: 4px 8px;
}
.input-row {
  display: flex;
  gap: 10px;
}
.input-row input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #d0d7de;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
}
.input-row input:focus { border-color: #667eea; }
.send-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}
.send-btn:hover { opacity: 0.9; }

.file-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.file-preview-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f0f2f5;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 13px;
}
.file-preview-item button {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal {
  background: white;
  border-radius: 16px;
  padding: 24px;
  max-width: 400px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}
.modal h2 { margin: 0 0 16px 0; font-size: 18px; }

.form-group { margin-bottom: 12px; }
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}
.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.friend-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.friend-option {
  padding: 6px 12px;
  border: 1px solid #d0d7de;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.friend-option:hover { background: #f0f2f5; }
.friend-option.selected {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 16px;
}
.cancel-btn {
  padding: 8px 20px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  background: white;
  cursor: pointer;
}
.save-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
}
.danger-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: #ff4d4f;
  color: white;
  cursor: pointer;
}

.group-info-item { padding: 6px 0; font-size: 14px; color: #333; }
.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.member-item {
  padding: 4px 12px;
  background: #f0f2f5;
  border-radius: 12px;
  font-size: 13px;
}
.owner-tag {
  color: #667eea;
  font-size: 11px;
  margin-left: 4px;
}

.modal-content img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 8px;
}

.group-avatar-edit {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}
.group-avatar-preview {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  cursor: pointer;
  overflow: hidden;
}
.group-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-edit-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0,0,0,0.5);
  color: white;
  font-size: 16px;
  text-align: center;
  padding: 4px 0;
  opacity: 0;
  transition: opacity 0.3s;
}
.group-avatar-preview:hover .avatar-edit-overlay {
  opacity: 1;
}
.edit-btn {
  padding: 4px 12px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 13px;
}
</style>