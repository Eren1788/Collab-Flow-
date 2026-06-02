import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：添加 Token
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers = config.headers || {}
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：处理 Blob 和普通 JSON
request.interceptors.response.use(
  (response) => {
    // 🔥 关键修改：如果是 blob 类型（文件下载），直接返回原始响应，不解析 JSON
    if (response.config.responseType === 'blob') {
      return response
    }
    // 普通 JSON 响应
    const result = response.data
    if (result.code !== 200) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(result.message)
    }
    return result
  },
  (error) => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export const requestWithType = <T = any>(config: AxiosRequestConfig) => {
  return request(config) as Promise<T>
}

export default request