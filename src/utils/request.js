// src/utils/request.js
import axios from 'axios';

const request = axios.create({
    baseURL: 'http://192.168.43.15:8080', // 后端地址
    timeout: 10000,
    withCredentials: true // 允许携带 Cookie，保持登录状态
});

export default request;