import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let isConnected = false
let messageHandlers = []

export const connectWebSocket = (onMessageReceived) => {
  console.log('connectWebSocket 被调用')
  
  const token = localStorage.getItem('token')
  console.log('Token:', token ? '存在' : '不存在')
  
  if (!token) {
    console.error('未登录，无法连接 WebSocket')
    return
  }

  if (onMessageReceived) {
    messageHandlers.push(onMessageReceived)
  }

  const socket = new WebSocket('http://192.168.43.15:8080/ws/chat')
  stompClient = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    debug: (str) => console.log('STOMP:', str),
    onConnect: () => {
      console.log('WebSocket 连接成功')
      isConnected = true
      
      // ✅ 订阅公共频道 /topic/messages
      stompClient.subscribe('/topic/messages', (message) => {
        const body = JSON.parse(message.body)
        console.log('📥 收到消息:', body)
        messageHandlers.forEach(handler => handler(body))
      })
    },
    onDisconnect: () => {
      console.log('WebSocket 断开')
      isConnected = false
    },
    onStompError: (frame) => {
      console.error('STOMP 错误:', frame)
    }
  })

  stompClient.activate()
}

export const sendMessage = (receiverId, content, senderId) => {
  if (!stompClient || !isConnected) {
    console.error('WebSocket 未连接')
    return false
  }

  const message = {
    senderId: senderId,
    receiverId: receiverId,
    content: content
  }

  stompClient.publish({
    destination: '/app/chat.send',
    body: JSON.stringify(message)
  })
  console.log('✅ 消息已发送:', message)
  return true
}

export const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    isConnected = false
    messageHandlers = []
  }
}

export const addMessageHandler = (handler) => {
  messageHandlers.push(handler)
}

export const removeMessageHandler = (handler) => {
  messageHandlers = messageHandlers.filter(h => h !== handler)
}