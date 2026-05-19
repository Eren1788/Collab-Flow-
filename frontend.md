# Collab Flow - 前端文档

---

## 1️⃣ 项目简介

Collab Flow 前端使用 **Vue3 + TypeScript + Vite + Element Plus + Pinia**  
提供企业级后台管理界面，与 Spring Boot 后端接口联调。

功能：

- 用户登录/退出
- 用户列表展示
- 项目增删改查
- 任务增删改查、指派执行人
- 响应式布局（Header + Sidebar + Footer）

---

## 2️⃣ 技术栈

| 技术 | 说明 |
|------|------|
| Vue 3 | 核心框架 (Composition API) |
| TypeScript | 类型安全 |
| Vite | 快速构建工具 |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| Axios | HTTP 请求封装 |
| Vue Router | 前端路由 |

---

## 3️⃣ 前端目录结构

```
src/
├── main.ts
├── App.vue
├── router/
│   └── index.ts
├── store/
│   └── user.ts
├── utils/
│   └── request.ts
├── views/
│   ├── Login.vue        # 登录页
│   ├── Home.vue         # 后台布局页 (集成 Header/Sidebar/Footer)
│   ├── UserList.vue     # 用户列表页
│   ├── ProjectList.vue  # 项目列表页
│   └── TaskList.vue     # 任务列表页
└── components/
    └── Layout/
        ├── Header.vue   # 顶部导航 + 用户信息 + 退出按钮
        ├── Sidebar.vue  # 左侧菜单导航
        └── Footer.vue   # 底部版权信息
```

---

## 4️⃣ 页面功能与逻辑

### 4.1 核心页面说明

| 页面 | 路径/组件 | 功能描述 |
|------|-----------|----------|
| 登录页 | Login.vue | 输入用户名密码，调用 `/user/login`，成功后保存 Token 并跳转 |
| 首页布局 | Home.vue | 集成布局组件，通过 `RouterView` 渲染子页面 |
| 用户管理 | UserList.vue | 展示用户列表，调用 `/user/list` |
| 项目管理 | ProjectList.vue | 项目的 CRUD 操作，状态管理 (0未开始/1进行中/2已完成) |
| 任务管理 | TaskList.vue | 任务的 CRUD 操作，支持从用户列表选择执行人 |

### 4.2 关键模块实现

#### Pinia 状态管理 (`store/user.ts`)
```typescript
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    info: {} as any
  }),
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info: any) {
      this.info = info
    },
    logout() {
      this.token = ''
      this.info = {}
      localStorage.removeItem('token')
    }
  }
})
```

#### Axios 请求封装 (`utils/request.ts`)
```typescript
import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { useUserStore } from '../store/user'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 5000
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
  res => res.data,
  err => Promise.reject(err)
)

export const requestWithType = <T = any>(config: AxiosRequestConfig) => {
  return request(config) as Promise<T>
}

export default request
```

---

## 5️⃣ 前端启动方式

```bash
npm install
npm run dev
```

访问地址：`http://localhost:5173/`

---

## 6️⃣ 项目亮点

- Vue 3 Composition API + TypeScript 现代化开发
- Pinia 全局状态管理与持久化
- Axios 拦截器统一处理鉴权 Token
- Element Plus 组件库快速构建 UI
- 模块化目录结构，易于维护与扩展