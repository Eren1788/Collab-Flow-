import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'  // 加上 type
import { useUserStore } from '../store/user'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 5000
})

// 请求拦截器
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if(userStore.token){
    config.headers = config.headers || {}
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器
// 添加泛型 T 表示返回数据类型
request.interceptors.response.use(
  (res) => res.data, 
  (err) => Promise.reject(err)
)

// 定义一个带泛型的请求方法
export const requestWithType = <T = any>(config: AxiosRequestConfig) => {
  return request(config) as Promise<T>
}

export default request