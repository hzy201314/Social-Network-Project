<template>
  <div class="group-chat-container">
    <!-- 左侧群聊列表 -->
    <div class="contact-list">
      <div class="contact-header">
        <h2>💬 群聊</h2>
        <button @click="showCreateGroup = true" class="create-btn">+</button>
      </div>
      <div class="contact-search">
        <input type="text" placeholder="搜索群聊" v-model="searchKeyword" />
      </div>
      <div class="contact-items">
        <div 
          v-for="group in filteredGroups" 
          :key="group.id"
          class="contact-item"
          :class="{ active: currentGroupId === group.id }"
          @click="selectGroup(group.id)"
        >
          <div class="contact-avatar">
            <span>{{ (group.name || '群').charAt(0) }}</span>
            <span v-if="group.unreadCount > 0" class="unread-badge">{{ group.unreadCount }}</span>
          </div>
          <div class="contact-info">
            <div class="contact-name">{{ group.name }}</div>
            <div class="contact-last-msg">{{ group.lastMessage || '' }}</div>
          </div>
          <div class="contact-time">{{ group.lastTime || '' }}</div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <div class="chat-header">
        <div class="chat-title">{{ currentGroup?.name || '选择群聊' }}</div>
        <div class="chat-actions">
          <button @click="showGroupInfo = true" class="info-btn">ℹ️</button>
        </div>
      </div>

      <!-- 消息列表 -->
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
            <span>{{ (msg.senderNickname || msg.senderUsername || '?').charAt(0) }}</span>
          </div>
          <div class="message-body">
            <div class="message-sender">{{ msg.senderNickname || msg.senderUsername }}</div>
            <div class="message-bubble">
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-row">
          <input 
            v-model="inputMessage" 
            @keyup.enter="sendGroupMessage" 
            placeholder="输入消息..."
          />
          <button @click="sendGroupMessage" class="send-btn">发送</button>
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

    <!-- ===== 群信息弹窗 ===== -->
    <div v-if="showGroupInfo" class="modal-overlay" @click.self="showGroupInfo = false">
      <div class="modal">
        <h2>群信息</h2>
        <div class="group-info-item"><strong>群名称：</strong>{{ currentGroup?.name }}</div>
        <div class="group-info-item"><strong>群主：</strong>{{ currentGroup?.ownerName }}</div>
        <div class="group-info-item"><strong>成员：</strong></div>
        <div class="member-list">
          <div v-for="member in groupMembers" :key="member.id" class="member-item">
            {{ member.nickname || member.username }}
            <span v-if="member.id === currentGroup?.ownerId" class="owner-tag">群主</span>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="leaveGroup" class="cancel-btn" v-if="!isOwner">退出群聊</button>
          <button @click="dissolveGroup" class="danger-btn" v-if="isOwner">解散群聊</button>
          <button @click="showGroupInfo = false" class="save-btn">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const currentUserId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)

// ===== 状态 =====
const groups = ref([])
const currentGroupId = ref(null)
const searchKeyword = ref('')
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messageListRef = ref(null)
const friends = ref([])

// ===== 弹窗 =====
const showCreateGroup = ref(false)
const showGroupInfo = ref(false)
const newGroupName = ref('')
const selectedFriends = ref([])
const groupMembers = ref([])

let pollingTimer = null

// ===== 计算属性 =====
const filteredGroups = computed(() => {
  if (!searchKeyword.value) return groups.value
  return groups.value.filter(g => g.name.includes(searchKeyword.value))
})

const currentGroup = computed(() => {
  return groups.value.find(g => g.id === currentGroupId.value)
})

const isOwner = computed(() => {
  return currentGroup.value?.ownerId === currentUserId.value
})

// ===== 加载好友列表 =====
const loadFriends = async () => {
  try {
    const res = await request.get('/api/friends')
    if (res.data.code === 0) {
      friends.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载好友失败:', error)
  }
}

// ===== 加载群列表 =====
const loadGroups = async () => {
  try {
    const res = await request.get('/api/groups')
    if (res.data.code === 0) {
      groups.value = (res.data.data || []).map(g => ({
        ...g,
        unreadCount: 0,
        lastMessage: '',
        lastTime: ''
      }))
      if (groups.value.length > 0 && !currentGroupId.value) {
        currentGroupId.value = groups.value[0].id
        loadMessages(groups.value[0].id)
      }
    }
  } catch (error) {
    console.error('加载群列表失败:', error)
  }
}

// ===== 加载群消息 =====
const loadMessages = async (groupId) => {
  if (!groupId) return
  loading.value = true
  try {
    const res = await request.get(`/api/groups/${groupId}/messages`)
    if (res.data.code === 0) {
      messages.value = res.data.data || []
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loading.value = false
  }
}

// ===== 选择群聊 =====
const selectGroup = async (groupId) => {
  if (currentGroupId.value === groupId) return
  currentGroupId.value = groupId
  await loadMessages(groupId)
}

// ===== 发送群消息 =====
const sendGroupMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content) return

  const groupId = currentGroupId.value
  if (!groupId) {
    alert('请先选择一个群聊')
    return
  }

  try {
    const res = await request.post(`/api/groups/${groupId}/messages`, {
      content: content
    })
    if (res.data.code === 0) {
      inputMessage.value = ''
      messages.value.push(res.data.data)
      await scrollToBottom()
      // 更新群列表最后消息
      const group = groups.value.find(g => g.id === groupId)
      if (group) {
        group.lastMessage = content
        group.lastTime = new Date().toLocaleTimeString()
      }
    } else {
      alert(res.data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送失败:', error)
    alert('发送失败，请重试')
  }
}

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
      await loadGroups()
    } else {
      alert(res.data.message || '创建失败')
    }
  } catch (error) {
    console.error('创建群聊失败:', error)
    alert('创建失败，请重试')
  }
}

// ===== 切换好友选择 =====
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
  if (!currentGroupId.value) return
  try {
    const res = await request.get(`/api/groups/${currentGroupId.value}/members`)
    if (res.data.code === 0) {
      groupMembers.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载群成员失败:', error)
  }
}

// ===== 退出群聊 =====
const leaveGroup = async () => {
  if (!confirm('确定要退出这个群聊吗？')) return
  try {
    const res = await request.delete(`/api/groups/${currentGroupId.value}/leave`)
    if (res.data.code === 0) {
      alert('已退出群聊')
      showGroupInfo.value = false
      await loadGroups()
    } else {
      alert(res.data.message || '退出失败')
    }
  } catch (error) {
    console.error('退出群聊失败:', error)
    alert('退出失败，请重试')
  }
}

// ===== 解散群聊 =====
const dissolveGroup = async () => {
  if (!confirm('确定要解散这个群聊吗？此操作不可撤销！')) return
  try {
    const res = await request.delete(`/api/groups/${currentGroupId.value}`)
    if (res.data.code === 0) {
      alert('群聊已解散')
      showGroupInfo.value = false
      await loadGroups()
    } else {
      alert(res.data.message || '解散失败')
    }
  } catch (error) {
    console.error('解散群聊失败:', error)
    alert('解散失败，请重试')
  }
}

// ===== 轮询新消息 =====
const pollNewMessages = async () => {
  if (!currentGroupId.value) return
  try {
    const res = await request.get(`/api/groups/${currentGroupId.value}/messages`)
    if (res.data.code === 0) {
      const newMessages = res.data.data || []
      if (newMessages.length !== messages.value.length) {
        messages.value = newMessages
        await scrollToBottom()
      }
    }
  } catch (error) {
    console.error('轮询消息失败:', error)
  }
}

// ===== 滚动到底部 =====
const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// ===== 格式化时间 =====
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// ===== 生命周期 =====
onMounted(async () => {
  await loadFriends()
  await loadGroups()
  pollingTimer = setInterval(pollNewMessages, 3000)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style scoped>
/* 样式和之前相同，只调整部分弹窗样式 */
.group-chat-container {
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
}
.message-self .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
.message-text { font-size: 14px; word-wrap: break-word; }
.message-time {
  font-size: 11px;
  color: #9aa6b5;
  text-align: right;
  margin-top: 4px;
}
.message-self .message-time { color: rgba(255, 255, 255, 0.7); }

.input-area {
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #e8ecf1;
  flex-shrink: 0;
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

/* ===== 弹窗样式 ===== */
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
</style>