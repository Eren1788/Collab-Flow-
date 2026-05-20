import { ElNotification } from 'element-plus'

class WebSocketService {

  private socket: WebSocket | null = null

  /**
   * 建立连接
   */
  connect(userId: number) {

    // 避免重复连接
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return
    }

    this.socket = new WebSocket(
      `ws://localhost:8080/ws/notification?userId=${userId}`
    )

    /**
     * 连接成功
     */
    this.socket.onopen = () => {

      console.log('WebSocket连接成功')
    }

    /**
     * 接收消息
     */
    this.socket.onmessage = (event) => {

      console.log('收到WebSocket消息：', event.data)

      const data = JSON.parse(event.data)

      ElNotification({
        title: '系统通知',
        message: data.content,
        type: 'success',
        duration: 3000
      })
    }

    /**
     * 连接关闭
     */
    this.socket.onclose = () => {

      console.log('WebSocket已断开')
    }

    /**
     * 连接异常
     */
    this.socket.onerror = (error) => {

      console.error('WebSocket异常', error)
    }
  }

  /**
   * 关闭连接
   */
  disconnect() {

    if (this.socket) {

      this.socket.close()

      this.socket = null
    }
  }

  /**
   * 发送消息
   */
  send(message: string) {

    if (
      this.socket &&
      this.socket.readyState === WebSocket.OPEN
    ) {

      this.socket.send(message)
    }
  }
}

export default new WebSocketService()