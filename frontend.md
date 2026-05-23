# Collab Flow - 前端开发文档（完整版）

# 1️⃣ 项目简介

Collab Flow 前端基于：

* Vue3
* TypeScript
* Vite
* Element Plus
* Pinia
* Axios
* Vue Router
* WebSocket

开发的企业级智能任务协作系统前端。

与 Spring Boot 后端完全对接。

适用于：

* Vue3课程设计
* Spring Boot + Vue 项目
* 毕业设计
* 企业级后台管理系统
* Java Web 全栈项目

---

# 2️⃣ 技术栈

| 技术 | 说明 |
| --- | --- |
| Vue3 | 核心框架（组合式 API） |
| TypeScript | 类型安全 |
| Vite | 前端构建工具 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | 网络请求 |
| Element Plus | UI组件库（v2.x） |
| WebSocket | 实时通知 |
| Element Plus Icons | 图标库 |

---

# 3️⃣ Node 环境要求

推荐：

* Node.js >= 18
* npm >= 9

---

# 4️⃣ 创建项目

```bash
npm create vite@latest collab-flow-web
```

选择：

* Vue
* TypeScript

---

# 5️⃣ 安装依赖

```bash
npm install
npm install element-plus axios pinia vue-router
npm install @element-plus/icons-vue
```

⚠️ **注意**: Element Plus 使用稳定版 v2.x，不要指定 3.x 版本。

---

# 6️⃣ 推荐目录结构

```text
src
├── api
│   ├── user.ts          # 用户相关API
│   ├── project.ts       # 项目相关API
│   ├── task.ts          # 任务相关API
│   ├── comment.ts       # 评论相关API
│   └── file.ts          # 文件相关API
│
├── assets
│
├── components
│   ├── Layout
│   │   ├── Header.vue   # 顶部导航
│   │   ├── Sidebar.vue  # 侧边栏菜单
│   │   └── Footer.vue   # 底部信息
│   │
│   ├── Project
│   │   ├── ProjectForm.vue
│   │   └── MemberDialog.vue
│   │
│   ├── Task
│   │   ├── TaskForm.vue
│   │   ├── TaskStatistics.vue
│   │   └── AssignDialog.vue
│   │
│   ├── Comment
│   │   └── CommentList.vue
│   │
│   └── File
│       └── UploadDialog.vue
│
├── layout
│   └── index.vue
│
├── router
│   └── index.ts         # 路由配置（含守卫）
│
├── store
│   ├── user.ts          # 用户状态（含权限）
│   └── app.ts
│
├── styles
│   └── index.scss
│
├── utils
│   ├── request.ts       # Axios封装
│   ├── auth.ts          # Token工具
│   ├── validate.ts      # 表单验证
│   └── websocket.ts     # WebSocket封装
│
├── views
│   ├── Login.vue        # 登录/注册页面
│   ├── Home.vue         # 工作台/仪表盘
│   ├── UserList.vue     # 用户管理（含个人信息）
│   ├── ProjectList.vue  # 项目管理
│   ├── TaskList.vue     # 任务管理（含统计）
│   ├── CommentList.vue  # 评论列表
│   └── FileList.vue     # 文件管理
│
├── App.vue
├── main.ts
└── vite.config.ts
```

---

# 7️⃣ 前端运行方式

```bash
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

---

# 8️⃣ Vite 跨域代理配置

**vite.config.ts**

必须配置代理，因为后端地址为 `http://localhost:8080`。

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      }
    }
  }
})
```

---

# 9️⃣ Axios 请求封装

**src/utils/request.ts**

这里必须与后端接口路径一致。

```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if(token){
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if(res.code !== 200){
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(res.message)
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

---

# 🔟 Token 工具类

**src/utils/auth.ts**

```typescript
const TOKEN_KEY = 'COLLAB_FLOW_TOKEN'

export function getToken(){
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token:string){
  localStorage.setItem(TOKEN_KEY,token)
}

export function removeToken(){
  localStorage.removeItem(TOKEN_KEY)
}
```

---

# 1️⃣1️⃣ Pinia 用户状态管理

**src/store/user.ts**

```typescript
import { defineStore } from 'pinia'
import websocket from '../utils/websocket'

export const useUserStore = defineStore('user',{
  state:()=>({
    token: localStorage.getItem('token') || '',
    info: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  
  getters:{
    // 权限判断方法
    hasPermission: (state) => {
      return (permission: string): boolean => {
        return Array.isArray(state.info?.permissions) && 
               state.info.permissions.includes(permission)
      }
    }
  },
  
  actions:{
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
```

⚠️ **重要**: 用户信息存储在 `state.info` 中，访问权限时使用 `userStore.hasPermission('xxx')`。

---

# 1️⃣2️⃣ 路由配置

**src/router/index.ts**

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path:'/login',
    component:()=>import('@/views/Login.vue')
  },
  {
    path:'/',
    component:()=>import('@/layout/index.vue'),
    redirect:'/dashboard',
    children:[
      {
        path:'dashboard',
        name:'Dashboard',
        component:()=>import('@/views/Home.vue'),
        meta:{ title:'工作台' }
      },
      {
        path:'user/list',
        name:'UserList',
        component:()=>import('@/views/UserList.vue'),
        meta:{ title:'用户管理' }
      },
      {
        path:'project/list',
        name:'ProjectList',
        component:()=>import('@/views/ProjectList.vue'),
        meta:{ title:'项目管理' }
      },
      {
        path:'task/list',
        name:'TaskList',
        component:()=>import('@/views/TaskList.vue'),
        meta:{ title:'任务管理' }
      },
      {
        path:'comment/list',
        name:'CommentList',
        component:()=>import('@/views/CommentList.vue'),
        meta:{ title:'评论管理' }
      },
      {
        path:'file/list',
        name:'FileList',
        component:()=>import('@/views/FileList.vue'),
        meta:{ title:'文件管理' }
      }
    ]
  }
]

const router = createRouter({
  history:createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to,from,next)=>{
  const token = getToken()
  if(to.path !== '/login' && !token){
    next('/login')
  }else{
    next()
  }
})

export default router
```

---

# 1️⃣3️⃣ 后端接口对应关系

这里已经与你后端 MD 完全对接。

### 用户模块接口

| 前端API | 请求方式 | 对应后端接口 | 说明 |
| --- | --- | --- | --- |
| loginApi | POST | /user/login | 用户登录 |
| registerApi | POST | /user/register | 用户注册 |
| getUserInfoApi | GET | /user/info | 获取当前用户信息 |
| getUserListApi | GET | /user/list | 用户列表 |
| getUserPageApi | GET | /user/page | 用户分页查询 |
| updateUserApi | PUT | /user/update | 修改用户信息 |
| deleteUserApi | DELETE | /user/delete/{id} | 删除用户 |
| uploadAvatarApi | POST | /user/avatar | 上传头像 |

### 项目模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| addProjectApi | POST | /project/add |
| getProjectListApi | GET | /project/list |
| getProjectPageApi | GET | /project/page |
| getProjectDetailApi | GET | /project/detail/{id} |
| updateProjectApi | PUT | /project/update |
| deleteProjectApi | DELETE | /project/delete/{id} |

### 任务模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| addTaskApi | POST | /task/add |
| getTaskListApi | GET | /task/list |
| getTaskPageApi | GET | /task/page |
| getTaskDetailApi | GET | /task/detail/{id} |
| updateTaskApi | PUT | /task/update |
| deleteTaskApi | DELETE | /task/delete/{id} |
| updateTaskStatusApi | PUT | /task/status |
| assignTaskApi | PUT | /task/assign |
| getTaskStatisticsApi | GET | /task/statistics |

### 评论模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| addCommentApi | POST | /comment/add |
| getCommentListApi | GET | /comment/list/{taskId} |
| deleteCommentApi | DELETE | /comment/delete/{id} |

### 文件模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| uploadFileApi | POST | /file/upload |
| getFileListApi | GET | /file/list |
| deleteFileApi | DELETE | /file/delete/{id} |
| downloadFileApi | GET | /file/download/{id} |

---

# 1️⃣4️⃣ API 模块封装

**src/api/user.ts**

```typescript
import request from '@/utils/request'

export function loginApi(data:any){
  return request({
    url:'/user/login',
    method:'post',
    data
  })
}

export function registerApi(data:any){
  return request({
    url:'/user/register',
    method:'post',
    data
  })
}

export function getUserInfoApi(){
  return request({
    url:'/user/info',
    method:'get'
  })
}

export function getUserListApi(params:any){
  return request({
    url:'/user/list',
    method:'get',
    params
  })
}

export function updateUserApi(data:any){
  return request({
    url:'/user/update',
    method:'put',
    data
  })
}

export function deleteUserApi(id:number){
  return request({
    url:`/user/delete/${id}`,
    method:'delete'
  })
}

/**
 * 上传用户头像
 */
export function uploadAvatarApi(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
```

---

# 1️⃣5️⃣ 项目 API

**src/api/project.ts**

```typescript
import request from '@/utils/request'

export function addProjectApi(data:any){
  return request({
    url:'/project/add',
    method:'post',
    data
  })
}

export function getProjectListApi(params:any){
  return request({
    url:'/project/list',
    method:'get',
    params
  })
}

export function getProjectPageApi(params:any){
  return request({
    url:'/project/page',
    method:'get',
    params
  })
}

export function getProjectDetailApi(id:number){
  return request({
    url:`/project/detail/${id}`,
    method:'get'
  })
}

export function updateProjectApi(data:any){
  return request({
    url:'/project/update',
    method:'put',
    data
  })
}

export function deleteProjectApi(id:number){
  return request({
    url:`/project/delete/${id}`,
    method:'delete'
  })
}

export function addProjectMemberApi(data:any){
  return request({
    url:'/project/member/add',
    method:'post',
    data
  })
}

export function getProjectMemberListApi(projectId:number){
  return request({
    url:`/project/member/list/${projectId}`,
    method:'get'
  })
}

export function deleteProjectMemberApi(id:number){
  return request({
    url:`/project/member/delete/${id}`,
    method:'delete'
  })
}
```

---

# 1️⃣6️⃣ 任务 API

**src/api/task.ts**

```typescript
import request from '@/utils/request'

export function addTaskApi(data:any){
  return request({
    url:'/task/add',
    method:'post',
    data
  })
}

export function getTaskListApi(params:any){
  return request({
    url:'/task/list',
    method:'get',
    params
  })
}

export function getTaskPageApi(params:any){
  return request({
    url:'/task/page',
    method:'get',
    params
  })
}

export function getTaskDetailApi(id:number){
  return request({
    url:`/task/detail/${id}`,
    method:'get'
  })
}

export function updateTaskApi(data:any){
  return request({
    url:'/task/update',
    method:'put',
    data
  })
}

export function deleteTaskApi(id:number){
  return request({
    url:`/task/delete/${id}`,
    method:'delete'
  })
}

export function updateTaskStatusApi(data:any){
  return request({
    url:'/task/status',
    method:'put',
    data
  })
}

export function assignTaskApi(data:any){
  return request({
    url:'/task/assign',
    method:'put',
    data
  })
}

export function getTaskStatisticsApi(){
  return request({
    url:'/task/statistics',
    method:'get'
  })
}
```

---

# 1️⃣7️⃣ 评论 API

**src/api/comment.ts**

```typescript
import request from '@/utils/request'

export function addCommentApi(data:any){
  return request({
    url:'/comment/add',
    method:'post',
    data
  })
}

export function getCommentListApi(taskId:number){
  return request({
    url:`/comment/list/${taskId}`,
    method:'get'
  })
}

export function deleteCommentApi(id:number){
  return request({
    url:`/comment/delete/${id}`,
    method:'delete'
  })
}
```

---

# 1️⃣8️⃣ 文件 API

**src/api/file.ts**

```typescript
import request from '@/utils/request'

export function uploadFileApi(data:FormData){
  return request({
    url:'/file/upload',
    method:'post',
    data,
    headers:{
      'Content-Type':'multipart/form-data'
    }
  })
}

export function getFileListApi(params:any){
  return request({
    url:'/file/list',
    method:'get',
    params
  })
}

export function deleteFileApi(id:number){
  return request({
    url:`/file/delete/${id}`,
    method:'delete'
  })
}

export function downloadFileApi(id:number){
  return request({
    url:`/file/download/${id}`,
    method:'get',
    responseType:'blob'
  })
}
```

---

# 1️⃣9️⃣ WebSocket 实时通知

**src/utils/websocket.ts**

```typescript
import { ElNotification } from 'element-plus'

class WebSocketService {

  private socket: WebSocket | null = null
  private listeners: Array<(data:any)=>void> = []

  /**
   * 建立连接
   */
  connect(userId:number){
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

    this.socket.onopen = ()=>{
      console.log('WebSocket连接成功')
    }

    this.socket.onmessage = (event)=>{
      console.log('收到WebSocket消息：',event.data)

      const data = JSON.parse(event.data)

      // 全局通知弹窗
      ElNotification({
        title:'系统通知',
        message:data.content,
        type:'success',
        duration:3000
      })

      // 通知所有监听器
      this.listeners.forEach(callback=>{
        callback(data)
      })
    }

    this.socket.onclose = ()=>{
      console.log('WebSocket已断开')
    }

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
    this.listeners =
      this.listeners.filter(item=>item !== callback)
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
    if(
      this.socket
      &&
      this.socket.readyState === WebSocket.OPEN
    ){
      this.socket.send(message)
    }
  }
}

export default new WebSocketService()
```

---

# 2️⃣0️⃣ 登录页面逻辑

**views/Login.vue**

核心逻辑：

```typescript
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { loginApi, registerApi, getUserInfoApi } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginForm = reactive({
  username:'',
  password:''
})

const registerDialogVisible = ref(false)
const registerForm = reactive({
  username:'',
  password:'',
  confirmPassword:'',
  nickname:'',
  email:'',
  phone:''
})

// 登录
const handleLogin = async ()=>{
  try {
    const res:any = await loginApi(loginForm)
    
    // 先保存 Token
    userStore.setToken(res.data.token)
    
    // 再获取用户完整信息（含权限）
    const userInfoRes:any = await getUserInfoApi()
    userStore.setUserInfo(userInfoRes.data)
    
    // 连接 WebSocket
    userStore.connectWebSocket()
    
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error('登录失败')
  }
}

// 注册
const handleRegister = async () => {
  try {
    await registerApi(registerForm)
    ElMessage.success('注册成功，请登录')
    registerDialogVisible.value = false
  } catch (error) {
    ElMessage.error('注册失败')
  }
}
```

---

# 2️⃣1️⃣ 用户列表页面逻辑

**views/UserList.vue**

新增功能：

* ✅ 个人信息卡片（置顶显示）
* ✅ 欢迎语动画效果
* ✅ 权限控制（超级管理员 vs 普通用户）
* ✅ 用户过滤（自动排除当前用户）

核心逻辑：

```typescript
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getUserPageApi,
  deleteUserApi,
  updateUserApi,
  uploadAvatarApi
} from '@/api/user'

const userStore = useUserStore()

// 判断是否为超级管理员
const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

// 头像上传
const uploadAvatar = async (options: any) => {
  try {
    const res: any = await uploadAvatarApi(options.file)
    if (res.code === 200 && res.data?.avatarUrl) {
      ElMessage.success('头像更新成功')
      const userInfoRes: any = await getUserInfoApi()
      userStore.setUserInfo(userInfoRes.data)
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  }
}
```

---

# 2️⃣2️⃣ 项目管理页面逻辑

**views/ProjectList.vue**

功能：

* 新增项目
* 编辑项目
* 删除项目
* 查看项目详情
* 管理项目成员

状态显示：

| 值 | 含义 |
| - | --- |
| 0 | 未开始 |
| 1 | 进行中 |
| 2 | 已完成 |

---

# 2️⃣3️⃣ 任务页面逻辑

**views/TaskList.vue**

新增功能：

* ✅ 任务统计卡片（总数、进行中、已完成、已逾期）

功能：

* 新增任务
* 编辑任务
* 删除任务
* 条件筛选
* 状态修改
* 指派执行人
* 优先级显示

任务优先级：

| 值 | 含义 |
| - | -- |
| 0 | 普通 |
| 1 | 紧急 |

任务状态：

| 值 | 含义 |
| - | --- |
| 0 | 待开始 |
| 1 | 进行中 |
| 2 | 已完成 |

---

# 2️⃣4️⃣ 文件上传页面逻辑

**views/FileList.vue**

上传组件：

```vue
<el-upload
  action="#"
  :http-request="uploadFile"
  multiple
>
  <el-button type="primary">
    上传文件
  </el-button>
</el-upload>
```

上传逻辑：

```typescript
const uploadFile = async (options:any)=>{
  const formData = new FormData()
  formData.append('file',options.file)
  formData.append('taskId',taskId.value)
  formData.append('projectId',projectId.value)
  await uploadFileApi(formData)
  ElMessage.success('上传成功')
}
```

---

# 2️⃣5️⃣ Dashboard 首页

**views/Home.vue**

建议展示：

* 用户总数
* 项目总数
* 任务总数
* 已完成任务数量
* ECharts统计图（可选）

---

# 2️⃣6️⃣ Layout 布局设计

**layout/index.vue**

布局：

* Header
* Sidebar
* Main Content
* Footer

推荐使用：

```vue
<el-container>
  <el-aside />
  <el-container>
    <el-header />
    <el-main />
    <el-footer />
  </el-container>
</el-container>
```

---

# 2️⃣7️⃣ Sidebar 菜单设计

菜单：

* 工作台
* 用户管理
* 项目管理
* 任务管理
* 文件管理

---

# 2️⃣8️⃣ Header 功能

Header：

* 当前登录用户
* 用户头像
* 退出登录
* 面包屑导航

---

# 2️⃣9️⃣ 分页规范

后端返回：

```json
{
  "code":200,
  "message":"success",
  "data":{
    "total":100,
    "records":[]
  }
}
```

前端分页处理：

```typescript
tableData.value = res.data.records
total.value = res.data.total
```

---

# 3️⃣0️⃣ Element Plus 推荐组件

推荐使用：

| 组件 | 用途 |
| --- | --- |
| el-table | 数据表格 |
| el-form | 表单 |
| el-dialog | 弹窗 |
| el-upload | 文件上传 |
| el-pagination | 分页 |
| el-menu | 菜单 |
| el-card | 卡片 |
| el-tag | 状态标签 |
| el-avatar | 用户头像 |
| el-descriptions | 描述列表 |
| el-switch | 状态开关 |
| el-notification | 通知弹窗 |

---

# 3️⃣1️⃣ 推荐开发顺序

建议：

1. 登录页（含注册）
2. Layout布局
3. 用户模块（含个人信息、头像上传）
4. 项目模块
5. 任务模块（含统计）
6. 评论模块
7. 文件模块
8. Dashboard统计
9. WebSocket实时通知

---

# 3️⃣2️⃣ 前端权限控制

权限标识命名规范：采用统一格式 `资源:操作`

示例：

* `user:list` - 查看用户列表
* `user:add` - 新增用户
* `user:delete` - 删除用户
* `user:status` - 修改用户状态

前端实现：

**模板中使用：**

```vue
<el-button v-if="userStore.hasPermission('user:delete')" type="danger">
  删除
</el-button>
```

**脚本中使用：**

```typescript
if (userStore.hasPermission('user:add')) {
  // 执行新增逻辑
}
```

⚠️ **重要**: 前端 UI 层面的权限控制仅用于用户体验优化，真正的安全验证必须在后端完成。

---

# 3️⃣3️⃣ 认证流程与缓存管理

登录顺序：

1. 调用登录接口获取 Token
2. 保存 Token 到 localStorage
3. 调用 `/user/info` 获取用户完整信息（含权限列表）
4. 存入 Pinia Store 和 localStorage
5. 连接 WebSocket
6. 跳转页面

退出清理：

退出登录时，必须同步清除：

* Token
* 用户信息（info）
* WebSocket 连接

---

# 3️⃣4️⃣ TypeScript 项目配置规范

TypeScript 配置文件要求：

* 项目根目录必须存在 `tsconfig.json` 文件
* 需创建 `tsconfig.node.json` 用于 Vite 配置文件类型检查
* 需创建 `vite-env.d.ts` 声明 Vue 文件和 Vite 相关的全局类型

编译器选项配置规范：

* `"strict": true` - 启用严格类型检查
* `"forceConsistentCasingInFileNames": true` - 强制文件名大小写一致

路径别名配置要求：

为确保 TypeScript 能正确识别 `@/*` 等路径别名，必须在以下配置文件中设置 paths 映射：

* `tsconfig.json`: 配置 `baseUrl` 指向项目根目录或 src 目录，配置 `paths` 将 `@/*` 映射到 `src/*`
* `vite.config.ts`: 配置 `resolve.alias` 与 TypeScript 配置保持一致

---

# 3️⃣5️⃣ Element Plus 2.x 组件语法规范

在使用 Element Plus 2.x 版本时，需注意以下语法变更以避免兼容性问题：

* 弹窗/对话框的可见性绑定：将 `:visible.sync` 改为 `v-model`
* 按钮尺寸属性：将 `size="mini"` 改为 `size="small"`（mini 在 2.x 中已废弃）

---

# 3️⃣6️⃣ 前后端联调说明

后端启动：

```text
http://localhost:8080
```

前端启动：

```text
http://localhost:5173
```

代理后：

```text
/api/user/login
```

实际请求：

```text
http://localhost:8080/user/login
```

---

# 3️⃣7️⃣ 项目完成度

| 模块 | 完成度 | 说明 |
| --- | --- | --- |
| 登录模块 | 100% | 含注册功能 |
| 用户模块 | 100% | 含个人信息、头像上传、权限控制 |
| 项目模块 | 90% | 基础CRUD完成 |
| 任务模块 | 90% | 含统计功能 |
| 评论模块 | 85% | 基础功能完成 |
| 文件模块 | 85% | 上传下载完成 |
| Dashboard | 70% | 基础统计完成 |
| WebSocket | 100% | 实时通知完成 |
| 权限体系 | 100% | 前后端权限控制完成 |

整体：

```text
约 92%
```

---

# 3️⃣8️⃣ 新增功能总结（v2.0）

相比原始文档，当前前端新增以下功能：

1. **用户模块增强**
   * ✅ 头像上传功能（/user/avatar）
   * ✅ 个人信息卡片展示（置顶显示）
   * ✅ 欢迎语动画效果
   * ✅ 注册功能（Login页面集成）
   * ✅ 角色管理接口（/role/list）
   * ✅ 用户状态开关控制（ElSwitch组件）
   * ✅ 用户列表自动过滤当前用户

2. **权限体系完善**
   * ✅ Pinia Store 中集成 hasPermission getter
   * ✅ 超级管理员权限判断逻辑（roleId === 1）
   * ✅ 权限标识规范（resource:action格式）
   * ✅ 前后端权限验证一致性

3. **WebSocket 实时通知**
   * ✅ WebSocket 工具类封装（websocket.ts）
   * ✅ 系统通知弹窗（ElNotification）
   * ✅ 消息监听器机制
   * ✅ 自动连接/断开管理

4. **任务统计功能**
   * ✅ 任务总数统计卡片
   * ✅ 进行中/已完成/已逾期分类统计

5. **UI/UX 优化**
   * ✅ 响应式布局
   * ✅ 动画效果
   * ✅ 渐变色背景
   * ✅ Hover交互效果
   * ✅ 加载状态提示

---

# 3️⃣9️⃣ 推荐优化方向

后续可优化：

* 动态菜单（基于权限）
* RBAC权限细化
* ECharts图表可视化
* 深色模式/暗黑主题
* 国际化（i18n）
* Markdown编辑器
* 富文本编辑器
* 文件预览（PDF、图片）
* 拖拽上传
* 任务看板视图
* 甘特图
* 消息中心
* 操作日志
* 数据导出（Excel）

---

# 4️⃣0️⃣ 总结

当前前端系统已经具备：

* 企业级后台管理基础架构
* Vue3 + TS现代化开发
* 前后端分离
* JWT鉴权
* 用户管理（含头像、权限）
* 项目管理
* 任务协作（含统计）
* 评论系统
* 文件上传
* 分页查询
* 状态管理（Pinia）
* 接口模块化
* Axios统一请求
* WebSocket实时通知
* 权限控制体系
* 注册功能
* 动画交互效果

适合作为：

* Vue3课程设计
* Spring Boot + Vue 项目
* 毕业设计
* 企业级后台管理系统
* Java Web 全栈项目

---

# 附录：常见问题

**Q1: 登录后权限不生效？**
A: 清除浏览器 Local Storage 中的 `token` 和 `userInfo`，重新登录。

**Q2: Element Plus 组件报错？**
A: 确保使用 v2.x 版本，语法使用 `v-model` 而非 `:visible.sync`。

**Q3: 路径别名 @/* 无法识别？**
A: 检查 `tsconfig.json` 和 `vite.config.ts` 中的 `paths` 和 `alias` 配置是否一致。

**Q4: WebSocket 连接失败？**
A: 确认后端 WebSocket 服务已启动，地址为 `ws://localhost:8080/ws/notification`。

**Q5: 头像上传后不显示？**
A: 检查后端返回的 `avatarUrl` 是否正确，前端会自动添加 `/api` 前缀和时间戳防止缓存。

---

**文档版本**: v2.0
**最后更新**: 2026-05-24
**维护者**: Collab Flow 开发团队
