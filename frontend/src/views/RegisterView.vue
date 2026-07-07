<template>
  <div class="register-container">
    <div class="register-card">
      <h2>注册新账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="registerForm.username" placeholder="3-20位字母或数字" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="registerForm.password" placeholder="至少6位" required />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input type="password" v-model="registerForm.confirmPassword" placeholder="再次输入密码" required />
        </div>
        <button type="submit" class="register-btn">注 册</button>
        <p class="login-link">
          已有账号？<a href="/login">去登录</a>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
});

const handleRegister = async () => {
  // 先检查密码和确认密码是否一致
  if (registerForm.password !== registerForm.confirmPassword) {
    alert('两次输入的密码不一致！')
    return
  }

  try {
    const res = await request.post('/api/auth/register', {
      username: registerForm.username,
      password: registerForm.password
    })

    if (res.data.code === 0) {
      alert('注册成功！请登录')
      router.push('/login')   // 跳转到登录页
    } else {
      alert(res.data.message) // 比如"用户名已存在"
    }
  } catch (error) {
    console.error('注册失败', error)
    alert('注册失败，请稍后重试')
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  position: fixed;
  top: 0;
  left: 0;
  margin: 0;
  padding: 0;
}
.register-card {
  background: white;
  padding: 100px;
  border-radius: 12px;
  box-shadow: 0 0px 24px rgba(255, 255, 255, 0.5), 0 8px 64px rgba(255, 255, 255, 0.5);
  width: 480px;
}
.register-card h2 {
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
  transition: border-color 0.3s;
}
.form-group input:focus {
  outline: none;
  border-color: #f5576c;
}
.register-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.3s;
}
.register-btn:hover {
  opacity: 0.9;
}
.login-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}
.login-link a {
  color: #f5576c;
  text-decoration: none;
}
</style>