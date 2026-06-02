import { ElNotification } from 'element-plus'

class WebSocketService {
  private socket: WebSocket | null = null
  private listeners: Array<(data: any) => void> = []
  private userId: number | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectDelay = 3000  // 重连间隔3秒
  private maxReconnectAttempts = 10
  private reconnectAttempts = 0

  connect(userId: number) {
    this.userId = userId
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return
    }
    this.socket = new WebSocket(`ws://localhost:8080/ws/notification?userId=${userId}`)
    this.socket.onopen = () => {
      console.log('WebSocket连接成功')
      this.reconnectAttempts = 0
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }
    this.socket.onmessage = (event) => {
      console.log('收到WebSocket消息：', event.data)
      const data = JSON.parse(event.data)
      const chatMessageTypes = ['NEW_COMMENT', 'NEW_PROJECT_COMMENT']
      if (!chatMessageTypes.includes(data.type)) {
        ElNotification({
          title: '系统通知',
          message: data.content,
          type: 'success',
          duration: 3000
        })
      }
      this.listeners.forEach(callback => callback(data))
    }
    this.socket.onclose = () => {
      console.log('WebSocket已断开，尝试重连...')
      this.scheduleReconnect()
    }
    this.socket.onerror = (error) => {
      console.error('WebSocket异常', error)
      this.socket?.close()
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('WebSocket重连次数已达上限，停止重连')
      return
    }
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++
      console.log(`第${this.reconnectAttempts}次尝试重连...`)
      if (this.userId) this.connect(this.userId)
    }, this.reconnectDelay)
  }

  addMessageListener(callback: (data: any) => void) {
    this.listeners.push(callback)
  }

  removeMessageListener(callback: (data: any) => void) {
    this.listeners = this.listeners.filter(item => item !== callback)
  }

  disconnect() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
  }

  send(message: string) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(message)
    }
  }
}

export default new WebSocketService()