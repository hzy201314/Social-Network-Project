// src/utils/upload.js
import request from '@/utils/request'

// ===== 上传单张图片 =====
export const uploadFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await request.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (res.data.code === 0) {
      return res.data.data  // 返回 URL
    } else {
      console.error('上传失败:', res.data.message)
      return null
    }
  } catch (error) {
    console.error('上传请求失败:', error)
    return null
  }
}

// ===== 上传多张图片 =====
export const uploadMultipleFiles = async (files) => {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })

  try {
    const res = await request.post('/api/files/upload/multiple', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (res.data.code === 0) {
      return res.data.data  // 返回 URL 列表
    } else {
      console.error('上传失败:', res.data.message)
      return []
    }
  } catch (error) {
    console.error('上传请求失败:', error)
    return []
  }
}