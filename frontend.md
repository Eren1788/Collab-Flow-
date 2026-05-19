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

开发的企业级智能任务协作系统前端。

与 Spring Boot 后端完全对接。

适用于：

* Vue3课程设计
* Spring Boot + Vue 项目
* 毕业设计
* 企业级后台管理系统
* Java Web 全栈项目

---

# 2️⃣ 前端技术栈

| 技术 | 说明 |
| --- | --- |
| Vue3 | 核心框架 |
| TypeScript | 类型安全 |
| Vite | 前端构建工具 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | 网络请求 |
| Element Plus | UI组件库 |
| ECharts | 图表统计（可选） |

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
npm install echarts
```

---

# 6️⃣ 推荐目录结构（完整版）

```text
src
├── api
│   ├── user.ts
│   ├── project.ts
│   ├── task.ts
│   ├── comment.ts
│   └── file.ts
│
├── assets
│
├── components
│   ├── Layout
│   │   ├── Header.vue
│   │   ├── Sidebar.vue
│   │   └── Footer.vue
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
│   └── index.ts
│
├── store
│   ├── user.ts
│   └── app.ts
│
├── styles
│   └── index.scss
│
├── utils
│   ├── request.ts
│   ├── auth.ts
│   └── validate.ts
│
├── views
│   ├── login
│   │   └── index.vue
│   │
│   ├── dashboard
│   │   └── index.vue
│   │
│   ├── user
│   │   ├── list.vue
│   │   └── profile.vue
│   │
│   ├── project
│   │   ├── list.vue
│   │   ├── detail.vue
│   │   └── member.vue
│   │
│   ├── task
│   │   ├── list.vue
│   │   ├── detail.vue
│   │   └── statistics.vue
│   │
│   ├── comment
│   │   └── index.vue
│   │
│   └── file
│       └── index.vue
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

vite.config.ts

必须配置代理。

因为后端：

```text
http://localhost:8080
```

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
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

# 9️⃣ Axios 请求封装（重要）

src/utils/request.ts

这里必须与后端接口路径一致。

```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from './auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = getToken()
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
    ElMessage.error(error.message)
    return Promise.reject(error)
  }
)

export default request
```

---

# 🔟 Token 工具类

src/utils/auth.ts

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

src/store/user.ts

```typescript
import { defineStore } from 'pinia'
import { loginApi } from '@/api/user'
import { setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user',{
  state:()=>({
    token:'',
    userInfo:{} as any
  }),
  actions:{
    async login(loginForm:any){
      const res:any = await loginApi(loginForm)
      this.token = res.data.token
      setToken(res.data.token)
    },
    logout(){
      this.token = ''
      this.userInfo = {}
      removeToken()
    }
  }
})
```

---

# 1️⃣2️⃣ 路由配置（完整版）

src/router/index.ts

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path:'/login',
    component:()=>import('@/views/login/index.vue')
  },
  {
    path:'/',
    component:()=>import('@/layout/index.vue'),
    redirect:'/dashboard',
    children:[
      {
        path:'dashboard',
        component:()=>import('@/views/dashboard/index.vue')
      },
      {
        path:'user/list',
        component:()=>import('@/views/user/list.vue')
      },
      {
        path:'project/list',
        component:()=>import('@/views/project/list.vue')
      },
      {
        path:'project/detail/:id',
        component:()=>import('@/views/project/detail.vue')
      },
      {
        path:'task/list',
        component:()=>import('@/views/task/list.vue')
      },
      {
        path:'task/detail/:id',
        component:()=>import('@/views/task/detail.vue')
      },
      {
        path:'task/statistics',
        component:()=>import('@/views/task/statistics.vue')
      },
      {
        path:'file/index',
        component:()=>import('@/views/file/index.vue')
      }
    ]
  }
]

const router = createRouter({
  history:createWebHistory(),
  routes
})

export default router
```

---# 1️⃣3️⃣ 后端接口对应关系（重点）

这里已经与你后端 MD 完全对接。

# 用户模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| loginApi | POST | /user/login |
| registerApi | POST | /user/register |
| getUserInfoApi | GET | /user/info |
| getUserListApi | GET | /user/list |
| getUserPageApi | GET | /user/page |
| getUserDetailApi | GET | /user/detail/{id} |
| updateUserApi | PUT | /user/update |
| deleteUserApi | DELETE | /user/delete/{id} |

---

# 项目模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| addProjectApi | POST | /project/add |
| getProjectListApi | GET | /project/list |
| getProjectPageApi | GET | /project/page |
| getProjectDetailApi | GET | /project/detail/{id} |
| updateProjectApi | PUT | /project/update |
| deleteProjectApi | DELETE | /project/delete/{id} |

---

# 项目成员接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| addProjectMemberApi | POST | /project/member/add |
| getProjectMemberListApi | GET | /project/member/list/{projectId} |
| deleteProjectMemberApi | DELETE | /project/member/delete/{id} |

---

# 任务模块接口

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

---

# 评论模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| addCommentApi | POST | /comment/add |
| getCommentListApi | GET | /comment/list/{taskId} |
| deleteCommentApi | DELETE | /comment/delete/{id} |

---

# 文件模块接口

| 前端API | 请求方式 | 对应后端接口 |
| --- | --- | --- |
| uploadFileApi | POST | /file/upload |
| getFileListApi | GET | /file/list |
| deleteFileApi | DELETE | /file/delete/{id} |
| downloadFileApi | GET | /file/download/{id} |

---# 1️⃣4️⃣ API 模块封装（必须与后端一致）

src/api/user.ts

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

export function getUserPageApi(params:any){
  return request({
    url:'/user/page',
    method:'get',
    params
  })
}

export function getUserDetailApi(id:number){
  return request({
    url:`/user/detail/${id}`,
    method:'get'
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
```

---# 1️⃣5️⃣ 项目 API

src/api/project.ts

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

---# 1️⃣6️⃣ 任务 API

src/api/task.ts

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

src/api/comment.ts

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

src/api/file.ts

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

# 1️⃣9️⃣ 登录页面逻辑

views/login/index.vue

核心逻辑：

```typescript
const loginForm = reactive({
  username:'',
  password:''
})

const handleLogin = async ()=>{
  const res:any = await loginApi(loginForm)
  userStore.setToken(res.data.token)
  router.push('/')
}
```

---

# 2️⃣0️⃣ 用户列表页面逻辑

views/user/list.vue

功能：

* 用户分页
* 删除用户
* 编辑用户
* 搜索用户

表格字段：

| 字段 | 说明 |
| --- | --- |
| id | 用户ID |
| username | 用户名 |
| nickname | 昵称 |
| role | 角色 |
| createTime | 创建时间 |

---

# 2️⃣1️⃣ 项目管理页面逻辑

views/project/list.vue

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

# 2️⃣2️⃣ 任务页面逻辑

views/task/list.vue

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
| - | --- |
| 0 | 普通 |
| 1 | 紧急 |

任务状态：

| 值 | 含义 |
| - | --- |
| 0 | 待开始 |
| 1 | 进行中 |
| 2 | 已完成 |

---

# 2️⃣3️⃣ 文件上传页面逻辑

views/file/index.vue

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

# 2️⃣4️⃣ Dashboard 首页

views/dashboard/index.vue

建议展示：

* 用户总数
* 项目总数
* 任务总数
* 已完成任务数量
* ECharts统计图

---

# 2️⃣5️⃣ Layout 布局设计

layout/index.vue

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

# 2️⃣6️⃣ Sidebar 菜单设计

菜单：

* 工作台
* 用户管理
* 项目管理
* 任务管理
* 文件管理

---

# 2️⃣7️⃣ Header 功能

Header：

* 当前登录用户
* 用户头像
* 退出登录
* 面包屑导航

---

# 2️⃣8️⃣ 分页规范（重要）

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

# 2️⃣9️⃣ Element Plus 推荐组件

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

---

# 3️⃣0️⃣ 推荐开发顺序

建议：

1. 登录页
2. Layout布局
3. 用户模块
4. 项目模块
5. 任务模块
6. 评论模块
7. 文件模块
8. Dashboard统计

---

# 3️⃣1️⃣ 前端权限控制（推荐）

建议：

```typescript
router.beforeEach((to,from,next)=>{
  const token = getToken()
  if(to.path !== '/login' && !token){
    next('/login')
  }else{
    next()
  }
})
```

---

# 3️⃣2️⃣ 推荐优化

后续可优化：

* 动态菜单
* RBAC权限
* WebSocket实时通知
* ECharts统计
* 深色模式
* 国际化
* 暗黑主题
* Markdown编辑器
* 富文本编辑器
* 文件预览
* 拖拽上传

---

# 3️⃣3️⃣ 前后端联调说明（重要）

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

# 3️⃣4️⃣ 项目完成度

| 模块 | 完成度 |
| --- | --- |
| 登录模块 | 90% |
| 用户模块 | 85% |
| 项目模块 | 80% |
| 任务模块 | 80% |
| 评论模块 | 70% |
| 文件模块 | 70% |
| Dashboard | 60% |

整体：

```text
约 80%
```

---

# 3️⃣5️⃣ 总结

当前前端系统已经具备：

* 企业级后台管理基础架构
* Vue3 + TS现代化开发
* 前后端分离
* JWT鉴权
* 用户管理
* 项目管理
* 任务协作
* 评论系统
* 文件上传
* 分页查询
* 状态管理
* 接口模块化
* Axios统一请求
* Pinia状态管理

适合作为：

* Vue3课程设计
* Spring Boot + Vue 项目
* 毕业设计
* 企业级后台管理系统
* Java Web 全栈项目


