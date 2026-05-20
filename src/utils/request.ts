import axios from 'axios'

import type { AxiosRequestConfig } from 'axios'

import { ElMessage } from 'element-plus'

import { useUserStore } from '../store/user'

const request = axios.create({

  baseURL: '/api',

  timeout: 10000
})

request.interceptors.request.use(config => {

  const userStore = useUserStore()

  if(userStore.token){

    config.headers = config.headers || {}

    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }

  return config
})

request.interceptors.response.use(

  (res) => {

    const result = res.data

    if(result.code !== 200){

      ElMessage.error(result.message || '请求失败')

      return Promise.reject(result.message)
    }

    return result
  },

  (err) => {

    ElMessage.error(err.message || '网络异常')

    return Promise.reject(err)
  }
)

export const requestWithType = <T = any>(config: AxiosRequestConfig) => {

  return request(config) as Promise<T>
}

export default request