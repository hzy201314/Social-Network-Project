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
      <textarea
        v-model="content"
        placeholder="分享你的想法..."
        class="content-input"
        maxlength="1000"
      ></textarea>
      <div class="char-count">{{ content.length }}/1000</div>

      <!-- 图片上传（简化版） -->
      <div class="image-upload">
        <input type="file" accept="image/*" @change="handleImageUpload" ref="fileInput" multiple />
        <div class="image-preview" v-if="images.length > 0">
          <div v-for="(img, index) in images" :key="index" class="image-item">
            <img :src="img" />
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

const router = useRouter()
const content = ref('')
const images = ref([])
const submitting = ref(false)
const fileInput = ref(null)

const goBack = () => {
  router.push('/home')
}

const handleImageUpload = (event) => {
  const files = event.target.files
  for (let file of files) {
    const reader = new FileReader()
    reader.onload = (e) => {
      images.value.push(e.target.result)
    }
    reader.readAsDataURL(file)
  }
}

const removeImage = (index) => {
  images.value.splice(index, 1)
}

const handlePublish = async () => {
  if (!content.value.trim() && images.value.length === 0) {
    alert('请输入内容或上传图片')
    return
  }

  submitting.value = true
  try {
    // 模拟发布
    await new Promise(resolve => setTimeout(resolve, 1000))
    alert('发布成功！')
    router.push('/home')
  } catch (error) {
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
  background: #f5f5f5;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  background: white;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.header h1 {
  font-size: 18px;
  margin: 0;
}

.back-btn, .submit-btn {
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.back-btn {
  background: #f0f0f0;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.publish-form {
  background: white;
  border-radius: 12px;
  padding: 16px;
}

.content-input {
  width: 100%;
  min-height: 200px;
  border: none;
  resize: vertical;
  font-size: 16px;
  outline: none;
  font-family: inherit;
}

.char-count {
  text-align: right;
  color: #999;
  font-size: 12px;
}

.image-upload {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
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
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.remove-img {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ff4444;
  color: white;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  cursor: pointer;
  font-size: 14px;
}
</style>