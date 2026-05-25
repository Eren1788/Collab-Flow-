import { ElNotification } from 'element-plus'

class WebSocketService {

  private socket: WebSocket | null = null

  /**
   * 消息监听器
   */
  private listeners: Array<(data:any)=>void> = []

  /**
   * 建立连接
   */
  connect(userId:number){

    /**
     * 避免重复连接
     */
    if(
      this.socket
      &&
      this.socket.readyState === WebSocket.OPEN
    ){
      return
    }

    this.socket = new WebSocket(
      `ws://localhost:8080/ws/notification?userId=${userId}`
    )

    /**
     * 连接成功
     */
    this.socket.onopen = ()=>{
      console.log('WebSocket连接成功')
    }

    /**
     * 接收消息
     */
    this.socket.onmessage = (event)=>{

      console.log('收到WebSocket消息：',event.data)

      const data = JSON.parse(event.data)

      /**
       * 全局通知弹窗（仅对非聊天消息弹窗，避免聊天消息重复弹窗）
       * 聊天消息类型：NEW_COMMENT（任务评论）、NEW_PROJECT_COMMENT（项目评论）
       * 其他类型如 TASK_ASSIGN、TASK_STATUS、QUESTION、REPLY 等才弹窗
       */
      const chatMessageTypes = ['NEW_COMMENT', 'NEW_PROJECT_COMMENT']
      if (!chatMessageTypes.includes(data.type)) {
        ElNotification({
          title: '系统通知',
          message: data.content,
          type: 'success',
          duration: 3000
        })
      }

      /**
       * 通知所有监听器（用于聊天界面实时更新）
       */
      this.listeners.forEach(callback=>{
        callback(data)
      })
    }

    /**
     * 关闭
     */
    this.socket.onclose = ()=>{
      console.log('WebSocket已断开')
    }

    /**
     * 异常
     */
    this.socket.onerror = (error)=>{
      console.error('WebSocket异常',error)
    }
  }

  /**
   * 注册监听器
   */
  addMessageListener(callback:(data:any)=>void){
    this.listeners.push(callback)
  }

  /**
   * 移除监听器
   */
  removeMessageListener(callback:(data:any)=>void){
    this.listeners = this.listeners.filter(item=>item !== callback)
  }

  /**
   * 断开连接
   */
  disconnect(){
    if(this.socket){
      this.socket.close()
      this.socket = null
    }
  }

  /**
   * 发送消息
   */
  send(message:string){
    if(this.socket && this.socket.readyState === WebSocket.OPEN){
      this.socket.send(message)
    }
  }
}

export default new WebSocketService()