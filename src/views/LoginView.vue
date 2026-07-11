<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>友趣 YouQu</h2>
        <p class="slogan">分享生活·发现兴趣</p>
      </div>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="loginForm.username" placeholder="请输入用户名" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="loginForm.password" placeholder="请输入密码" required />
        </div>
        <button type="submit" class="login-btn">登 录</button>
        <p class="register-link">
          还没有账号？<a href="/register">立即注册</a>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    alert('请输入用户名和密码')
    return
  }

  try {
    const res = await request.post('/api/auth/login', {
      username: loginForm.username,
      password: loginForm.password
    })

    if (res.data.code === 0) {
      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.data.user))
      alert('登录成功！')
      router.push('/home')
    } else {
      alert(res.data.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败', error)
    alert('登录失败，请检查网络')
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100vw;
  background-image: url('@/assets/login-bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: fixed;
  top: 0;
  left: 0;
  margin: 0;
  padding: 0;
}

.login-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 44px 36px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(118, 75, 162, 0.08), 0 4px 12px rgba(255, 255, 255, 0.3);
  width: 100%;
  max-width: 400px;
  min-height: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h2 {
  margin: 0 0 4px 0;
  font-size: 28px;
  color: #4a5568;
  font-weight: 700;
  letter-spacing: 1px;
}
.login-header .slogan {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  font-weight: 400;
  letter-spacing: 4px;
}

.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #6b7280;
  font-size: 14px;
}
.form-group input {
  width: 100%;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  font-size: 14px;
  color: #333;
  box-sizing: border-box;
  transition: all 0.3s ease;
}
.form-group input::placeholder {
  color: #a0aec0;
}
.form-group input:focus {
  outline: none;
  border-color: rgba(255, 182, 193, 0.6);
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 4px rgba(255, 182, 193, 0.1);
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #fbc2eb 0%, #a6c1ee 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 2px;
  box-shadow: 0 4px 12px rgba(251, 194, 235, 0.2);
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(251, 194, 235, 0.4);
  opacity: 0.9;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #718096;
}
.register-link a {
  color: #b39ddb;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}
.register-link a:hover {
  color: #9575cd;
}
</style>