<template>
  <div class="comment-wrapper">
    <div class="comment-item" :style="{ paddingLeft: level * 24 + 'px' }">
      <div class="comment-user-info">
        <div class="comment-avatar">
          <img 
            v-if="getUserAvatar(comment.userId, comment.avatar, comment.username)" 
            :src="getUserAvatar(comment.userId, comment.avatar, comment.username)" 
            alt="头像" 
          />
          <span v-else>{{ (getUserNickname(comment.userId, comment.nickname, comment.username)).charAt(0) }}</span>
        </div>
        <span class="comment-user">{{ getUserNickname(comment.userId, comment.nickname, comment.username) }}</span>
        <span v-if="comment.isPending" class="pending-tag">发布中...</span>
      </div>

      <!-- 评论内容 -->
      <span class="comment-content">{{ comment.content }}</span>
      <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>

      <!-- 回复按钮 -->
      <button 
        @click="$emit('reply', comment.id, comment.username)" 
        class="reply-btn"
        :disabled="!comment.isReady"
        :style="{ opacity: comment.isReady ? 1 : 0.5, cursor: comment.isReady ? 'pointer' : 'not-allowed' }"
      >
        回复
      </button>

      <!-- 删除按钮 -->
      <button 
        v-if="showDelete && !comment.isDeleted && isOwner(comment)"
        @click="confirmDelete(comment.id)"
        class="delete-btn"
      >
        删除
      </button>
    </div>

    <!-- 递归渲染子评论 -->
    <div v-if="comment.replies && comment.replies.length > 0" class="replies-wrapper">
      <CommentTree
        v-for="child in comment.replies"
        :key="child.id"
        :comment="child"
        :level="level + 1"
        :show-delete="showDelete"
        @reply="(id, username) => $emit('reply', id, username)"
        @delete="(id) => $emit('delete', id)"
      />
    </div>
  </div>
</template>

<script setup>
import { formatTime } from '@/utils/dateUtils'
import { getUserAvatar, getUserNickname } from '@/utils/avatar'

defineProps({
  comment: {
    type: Object,
    required: true
  },
  level: {
    type: Number,
    default: 0
  },
  showDelete: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['reply', 'delete'])

// ✅ 判断是否是当前用户的评论
const isOwner = (comment) => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return false
  try {
    const user = JSON.parse(userStr)
    return user.id === comment.userId
  } catch (e) {
    return false
  }
}

// ✅ 确认删除
const confirmDelete = (commentId) => {
  if (confirm('确定要删除这条评论吗？')) {
    emit('delete', commentId)
  }
}
</script>

<style scoped>
.comment-wrapper {
  /* 保持原有样式 */
}

.comment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 14px;
  color: #333;
  flex-wrap: wrap;
}

.comment-user-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.comment-user {
  font-weight: 600;
  color: #1f2a3a;
}

.comment-content {
  color: #333;
}

.comment-time {
  font-size: 11px;
  color: #9aa6b5;
  margin-left: 4px;
}

.reply-btn {
  background: none;
  border: none;
  color: #9aa6b5;
  cursor: pointer;
  font-size: 12px;
  margin-left: 4px;
}

.reply-btn:hover:not(:disabled) {
  color: #667eea;
}

.reply-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pending-tag {
  font-size: 11px;
  color: #f59e0b;
  background: #fef3c7;
  padding: 0 8px;
  border-radius: 10px;
  margin-left: 4px;
}

.delete-btn {
  background: none;
  border: none;
  color: #9aa6b5;
  cursor: pointer;
  font-size: 12px;
  margin-left: 4px;
  padding: 0 4px;
  transition: color 0.2s;
}

.delete-btn:hover {
  color: #d0314e;
}

.replies-wrapper {
  /* 保持原有样式 */
}
</style>