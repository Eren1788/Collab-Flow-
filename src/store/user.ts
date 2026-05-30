import { defineStore } from 'pinia'
import websocket from '../utils/websocket'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    info: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  getters: {
    hasPermission: (state) => {
      return (permission: string): boolean => {
        return Array.isArray(state.info?.permissions) && state.info.permissions.includes(permission)
      }
    }
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info: any) {
      this.info = info
      localStorage.setItem('userInfo', JSON.stringify(info))
      // 用户信息设置后，如果已登录则建立WebSocket连接
      if (info?.id) {
        this.connectWebSocket()
      }
    },
    connectWebSocket() {
      if (this.info?.id) {
        websocket.connect(this.info.id)
      }
    },
    logout() {
      this.token = ''
      this.info = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      websocket.disconnect()
    }
  }
})