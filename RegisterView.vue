<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>友趣 YouQu</h2>
        <p class="slogan">分享生活·发现兴趣</p>
      </div>
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
      router.push('/login')
    } else {
      alert(res.data.message)
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

.register-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 44px 36px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(118, 75, 162, 0.08), 0 4px 12px rgba(255, 255, 255, 0.3);
  width: 100%;
  max-width: 420px;
  min-height: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}
.register-header h2 {
  margin: 0 0 4px 0;
  font-size: 28px;
  color: #4a5568;
  font-weight: 700;
  letter-spacing: 1px;
}
.register-header .slogan {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  font-weight: 400;
  letter-spacing: 4px;
}

.form-group {
  margin-bottom: 18px;
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

.register-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #f8e8ff 0%, #fde2e4 100%);
  color: #6b3fa0;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 2px;
  box-shadow: 0 4px 12px rgba(253, 194, 235, 0.2);
}
.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(253, 194, 235, 0.4);
  opacity: 0.9;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #718096;
}
.login-link a {
  color: #6b3fa0;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}
.login-link a:hover {
  color: #5b2e8a;
}
</style>