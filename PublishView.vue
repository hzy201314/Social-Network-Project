<template>
  <div class="publish-container">
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>发布动态</h1>
      <button @click="handlePublish" class="submit-btn" :disabled="submitting">
        {{ submitting ? '发布中...' : '发布' }}
      </button>
    </div>

    <div class="publish-form">
      <!-- ✅ 标题输入 -->
      <input
        v-model="title"
        type="text"
        placeholder="给动态加个标题（选填）"
        class="title-input"
        maxlength="50"
      />
      
      <textarea
        v-model="content"
        placeholder="分享你的想法..."
        class="content-input"
        maxlength="1000"
      ></textarea>
      <div class="char-count">{{ content.length }}/1000</div>

      <!-- ✅ 标签输入 -->
      <div class="tag-input-area">
        <input
          v-model="tagInput"
          type="text"
          placeholder="输入标签，按回车添加（如：美食）"
          class="tag-input"
          @keyup.enter="addTag"
        />
        <div class="tag-list">
          <span v-for="(tag, index) in tags" :key="index" class="tag-item">
            #{{ tag }}
            <button @click="removeTag(index)" class="remove-tag">×</button>
          </span>
        </div>
      </div>

      <!-- 图片上传 -->
      <div class="image-upload">
        <input 
          type="file" 
          accept="image/*" 
          multiple 
          @change="handleImageSelect" 
          ref="fileInput"
          style="display:none"
        />
        <button @click="$refs.fileInput.click()" class="upload-btn">📷 添加图片</button>
        <div class="image-preview" v-if="previewImages.length > 0">
          <div v-for="(img, index) in previewImages" :key="index" class="image-item">
            <img :src="img" alt="预览" />
            <button @click="removeImage(index)" class="remove-img">×</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { uploadMultipleFiles } from '@/utils/upload'

const router = useRouter()
const title = ref('')
const content = ref('')
const tags = ref([])
const tagInput = ref('')
const previewImages = ref([])
const imageFiles = ref([])
const submitting = ref(false)
const fileInput = ref(null)

const goBack = () => router.push('/home')

// ===== 标签操作 =====
const addTag = () => {
  const text = tagInput.value.trim()
  if (text && !tags.value.includes(text) && tags.value.length < 5) {
    tags.value.push(text)
    tagInput.value = ''
  } else if (tags.value.length >= 5) {
    alert('最多添加5个标签')
  }
}

const removeTag = (index) => {
  tags.value.splice(index, 1)
}

// ===== 图片操作 =====
const handleImageSelect = (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  files.forEach(file => {
    if (!file.type.startsWith('image/')) {
      alert('请上传图片文件')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过5MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      imageFiles.value.push(file)
      previewImages.value.push(e.target.result)
    }
    reader.readAsDataURL(file)
  })
  event.target.value = ''
}

const removeImage = (index) => {
  previewImages.value.splice(index, 1)
  imageFiles.value.splice(index, 1)
}

// ===== 发布动态 =====
const handlePublish = async () => {
  if (!content.value.trim() && previewImages.value.length === 0 && !title.value.trim()) {
    alert('请输入内容或标题')
    return
  }

  submitting.value = true
  try {
    let imageUrls = []
    if (imageFiles.value.length > 0) {
      imageUrls = await uploadMultipleFiles(imageFiles.value)
      if (imageUrls.length === 0 && imageFiles.value.length > 0) {
        alert('图片上传失败，请重试')
        submitting.value = false
        return
      }
    }

    const res = await request.post('/api/posts', {
      title: title.value.trim() || undefined,
      content: content.value.trim(),
      images: imageUrls,
      tags: tags.value
    })

    if (res.data.code === 0) {
      alert('发布成功！🎉')
      router.push('/home')
    } else {
      alert(res.data.message || '发布失败')
    }
  } catch (error) {
    console.error('发布失败:', error)
    alert('发布失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 16px;
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  margin-bottom: 16px;
}
.header h1 { font-size: 18px; margin: 0; }
.back-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #667eea;
  padding: 4px 8px;
}
.submit-btn {
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.publish-form {
  background: white;
  border-radius: 12px;
  padding: 16px;
}

.title-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  outline: none;
  margin-bottom: 12px;
  box-sizing: border-box;
}
.title-input:focus { border-color: #667eea; }

.content-input {
  width: 100%;
  min-height: 150px;
  border: none;
  resize: vertical;
  font-size: 16px;
  outline: none;
  font-family: inherit;
}
.char-count { text-align: right; color: #999; font-size: 12px; }

.tag-input-area {
  border-top: 1px solid #f0f2f5;
  padding-top: 12px;
  margin-top: 8px;
}
.tag-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}
.tag-input:focus { border-color: #667eea; }

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.tag-item {
  background: #e8ecf1;
  color: #667eea;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.remove-tag {
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
  font-size: 14px;
  padding: 0 2px;
}

.image-upload { margin-top: 12px; border-top: 1px solid #f0f2f5; padding-top: 12px; }
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
</style>