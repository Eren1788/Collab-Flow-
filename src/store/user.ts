import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    info: {} as any
  }),
  actions: {
    setToken(token: string){
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info: any){
      this.info = info
    },
    logout(){
      this.token = ''
      this.info = {}
      localStorage.removeItem('token')
    }
  }
})