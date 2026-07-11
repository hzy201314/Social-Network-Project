<template>
  <div class="login-container">
    <div class="login-card">
      <h2>社交网络平台</h2>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: fixed;
  top: 0;
  left: 0;
  margin: 0;
  padding: 0;
}
.login-card {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 0 30px 10px rgba(0,  0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  min-height: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: #555;
}
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}
.form-group input:focus {
  outline: none;
  border-color: #667eea;
}
.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.3s;
}
.login-btn:hover {
  opacity: 0.9;
}
.register-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}
.register-link a {
  color: #667eea;
  text-decoration: none;
}
</style>