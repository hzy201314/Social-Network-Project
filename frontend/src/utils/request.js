import axios from 'axios'

const request = axios.create({
  baseURL: 'http://192.168.43.15:8080', // 改成你的后端IP
  timeout: 30000
})

// ✅ 请求拦截器：必须返回 config
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    console.log('🔑 Token:', token ? '存在' : '不存在')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('✅ 已添加 Authorization 头')
    }
    return config  // ✅ 这行必须存在！
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：处理 Token 过期
request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request