import { defineStore } from 'pinia'

import websocket from '../utils/websocket'

export const useUserStore = defineStore('user', {

  state: () => ({
    token: localStorage.getItem('token') || '',
    info: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),

  getters: {
    //新增 hasPermission getter
    hasPermission: (state) => {
      return (permission: string): boolean => {
        return Array.isArray(state.info?.permissions) && 
               state.info.permissions.includes(permission)
      }
    }
  },

  actions: {
    // ... 原有 actions 不变
    setToken(token: string){
      this.token = token
      localStorage.setItem('token', token)
    },

    setUserInfo(info: any){
      this.info = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },

    connectWebSocket(){
      if(this.info?.id){
        websocket.connect(this.info.id)
      }
    },

    logout(){
      this.token = ''
      this.info = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      websocket.disconnect()
    }
  }
})