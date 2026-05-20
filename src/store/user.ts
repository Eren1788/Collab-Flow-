import { defineStore } from 'pinia'

import websocket from '../utils/websocket'

export const useUserStore = defineStore('user', {

  state: () => ({

    token: localStorage.getItem('token') || '',

    info: JSON.parse(localStorage.getItem('userInfo') || '{}')

  }),

  actions: {

    /**
     * 设置token
     */
    setToken(token: string){

      this.token = token

      localStorage.setItem('token', token)
    },

    /**
     * 设置用户信息
     */
    setUserInfo(info: any){

      this.info = info

      localStorage.setItem(
        'userInfo',
        JSON.stringify(info)
      )
    },

    /**
     * 建立WebSocket连接
     */
    connectWebSocket(){

      if(this.info?.id){

        websocket.connect(this.info.id)
      }
    },

    /**
     * 退出登录
     */
    logout(){

      this.token = ''

      this.info = {}

      localStorage.removeItem('token')

      localStorage.removeItem('userInfo')

      websocket.disconnect()
    }
  }
})