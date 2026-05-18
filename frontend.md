# Collab Flow - 前端文档

---

## 1️⃣ 项目简介

Collab Flow 前端使用 **Vue3 + Vite + Element Plus + Pinia + Axios**  
提供后台管理界面，支持：

- 登录、Token 存储
- 首页布局 + 左侧菜单 + 路由
- 用户列表、项目列表、任务列表
- 新增/编辑/删除功能
- 前后端接口调用

---

## 2️⃣ 技术栈

| 技术 | 说明 |
|------|------|
| Vue3 | 前端核心框架 |
| Vite | 构建工具 |
| Element Plus | UI组件库 |
| Pinia | 状态管理 |
| Axios | API 请求 |
| TypeScript | 类型约束 |
| Vue Router | 路由管理 |

---

## 3️⃣ 前端目录结构

```text
src/
├── main.ts
├── App.vue
├── router/index.ts
├── store/user.ts
├── utils/request.ts
├── views/
│   ├── Login.vue
│   ├── Home.vue
│   ├── UserList.vue
│   ├── ProjectList.vue
│   └── TaskList.vue
└── components/Layout/
    ├── Header.vue
    └── Sidebar.vue
```

---

## 4️⃣ 页面功能说明

### 4.1 登录页 (Login.vue)

- 用户输入用户名和密码
- 调用 `/user/login` 接口
- 成功后存储 token 并跳转首页

### 4.2 首页 (Home.vue)

- 顶部 Header + 左侧 Sidebar + 内容区域
- 通过 `<router-view>` 渲染列表页

### 4.3 用户列表 (UserList.vue)

- 表格显示用户信息
- 调用 `/user/list` 接口

### 4.4 项目管理 (ProjectList.vue)

- 表格显示项目
- 支持新增、编辑、删除
- 调用 `/project/add`, `/project/update`, `/project/delete/{id}`

### 4.5 任务管理 (TaskList.vue)

- 表格显示任务
- 支持新增、编辑、删除
- 调用 `/task/add`, `/task/update`, `/task/delete/{id}`
- 可以选择执行人（调用 `/user/list`）

---

## 5️⃣ 核心代码示例

### 5.1 Axios 封装

```typescript
import axios from 'axios'
import { useUserStore } from '../store/user'

const request = axios.create({ baseURL: 'http://localhost:8080', timeout: 5000 })

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if(userStore.token){
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(res => res.data, err => Promise.reject(err))

export default request
```

### 5.2 接口调用示例

```typescript
// 登录
const loginRes = await request.post('/user/login', {username:'admin', password:'123456'})

// 获取用户列表
const users = await request.get('/user/list')

// 新增项目
await request.post('/project/add', {name:'新项目', description:'测试', status:0})

// 新增任务
await request.post('/task/add', {title:'任务1', content:'描述', status:0, executorId:1, projectId:1})
```

---

## 6️⃣ 前端启动方式

```bash
npm install
npm run dev
```

访问页面：http://localhost:5173

---

## 7️⃣ 项目亮点

- 完整后台管理模板
- 登录鉴权 + Token 自动存储
- 左侧菜单 + 路由守卫
- CRUD 操作示例
- 前后端联调示例
