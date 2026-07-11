// src/utils/avatar.js

// 获取当前用户信息
export const getCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch (e) {
      return {}
    }
  }
  return {}
}

// 获取用户头像（优先从 localStorage 获取）
export const getUserAvatar = (userId, postAvatar, postUsername) => {
  // 如果是当前用户，从 localStorage 获取最新头像
  const currentUser = getCurrentUser()
  if (userId === currentUser.id) {
    return currentUser.avatar || null
  }
  // 非当前用户，使用 post 数据中的 avatar
  return postAvatar || null
}

// 获取用户昵称（优先从 localStorage 获取）
export const getUserNickname = (userId, postNickname, postUsername) => {
  const currentUser = getCurrentUser()
  if (userId === currentUser.id) {
    return currentUser.nickname || postNickname || postUsername || '匿名用户'
  }
  return postNickname || postUsername || '匿名用户'
}