# Collab Flow - 企业级协作管理平台

---

## 1️⃣ 项目简介

Collab Flow 是一个企业级任务协作管理平台，功能类似简化版 Jira/Trello，主要面向团队项目管理。  
主要功能包括：

- 用户注册、登录、信息管理（JWT 鉴权）
- 项目增删改查
- 任务增删改查、状态管理、指派执行人
- 评论功能
- 文件上传
- 多角色权限控制（管理员/普通用户）

> 本项目前后端分离，前端使用 Vue3 + Vite + Element Plus + Pinia，后端使用 Spring Boot + MyBatis Plus + MySQL + Redis。

---

## 2️⃣ 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue3 + Vite + Element Plus + Pinia + Axios |
| 后端 | Spring Boot 3 + MyBatis Plus + Lombok + JWT + Redis |
| 数据库 | MySQL 8 |
| 文档 | Knife4j（OpenAPI 3.0） |
| 构建工具 | Maven + Node.js + npm |

---

## 3️⃣ 后端数据库表设计

### 用户表 `user`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像URL |
| role | VARCHAR(20) | 角色：admin/user |
| create_time | DATETIME | 创建时间 |

### 项目表 `project`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(100) | 项目名称 |
| description | TEXT | 项目描述 |
| creator_id | BIGINT | 创建人ID |
| status | TINYINT | 状态：0未开始 1进行中 2已完成 |
| create_time | DATETIME | 创建时间 |

### 任务表 `task`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 所属项目ID |
| title | VARCHAR(100) | 任务标题 |
| content | TEXT | 任务内容 |
| creator_id | BIGINT | 创建人ID |
| executor_id | BIGINT | 执行人ID |
| status | TINYINT | 任务状态：0待开始 1进行中 2完成 |
| priority | TINYINT | 优先级：0普通 1紧急 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| create_time | DATETIME | 创建时间 |

### 评论表 `comment`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 任务ID |
| user_id | BIGINT | 评论人ID |
| content | TEXT | 评论内容 |
| create_time | DATETIME | 评论时间 |

### 文件表 `file`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 关联任务ID |
| project_id | BIGINT | 关联项目ID |
| name | VARCHAR(100) | 文件名 |
| url | VARCHAR(255) | 文件存储URL |
| uploader_id | BIGINT | 上传人ID |
| upload_time | DATETIME | 上传时间 |

### 角色权限表

- `role` 表：角色表（admin/user）
- `permission` 表：权限表（user:add, project:edit等）
- `role_permission` 表：角色权限关联
- `user_role` 表：用户角色关联

---

## 4️⃣ 后端接口说明

### 用户模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /user/login | POST | `{ "username": "xxx", "password": "xxx" }` | `{ "code":200,"data":"token" }` | 用户登录，返回 JWT token |
| /user/register | POST | `{ "username":"xxx","password":"xxx","nickname":"xxx" }` | `{ "code":200 }` | 用户注册 |
| /user/info | GET | Header Authorization | `{ "code":200,"data":{id,username,nickname,role}}` | 获取用户信息 |
| /user/list | GET | Authorization | `{ "code":200,"data":[...] }` | 用户列表 |

### 项目模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /project/add | POST | `{ "name":"xxx","description":"xxx","status":0 }` | `{ "code":200 }` | 新增项目 |
| /project/list | GET | Authorization | `{ "code":200,"data":[...] }` | 项目列表 |
| /project/update | PUT | `{ "id":1,"name":"xxx" }` | `{ "code":200 }` | 编辑项目 |
| /project/delete/{id} | DELETE | Authorization | `{ "code":200 }` | 删除项目 |

### 任务模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /task/add | POST | `{ "title":"xxx","content":"xxx","status":0,"executorId":1,"projectId":1 }` | `{ "code":200 }` | 新增任务 |
| /task/list | GET | Authorization | `{ "code":200,"data":[...] }` | 任务列表 |
| /task/update | PUT | `{ "id":1,"title":"xxx" }` | `{ "code":200 }` | 编辑任务 |
| /task/delete/{id} | DELETE | Authorization | `{ "code":200 }` | 删除任务 |

### 评论模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /task/comment/add | POST | `{ "taskId":1,"content":"xxx" }` | `{ "code":200 }` | 新增评论 |

### 文件模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /file/upload | POST | multipart/form-data file | `{ "code":200,"data":"文件URL" }` | 文件上传 |

---

## 5️⃣ 前端目录结构

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

## 6️⃣ 前端页面功能说明

### 6.1 核心页面逻辑

| 页面 | 组件名 | 功能描述 |
|------|--------|----------|
| 登录页 | Login.vue | 输入用户名密码，调用 `/user/login`，成功后保存 Token 并跳转 |
| 首页布局 | Home.vue | 集成布局组件，通过 `RouterView` 渲染子页面 |
| 用户管理 | UserList.vue | 展示用户列表，调用 `/user/list` |
| 项目管理 | ProjectList.vue | 项目的 CRUD 操作，状态管理 (0未开始/1进行中/2已完成) |
| 任务管理 | TaskList.vue | 任务的 CRUD 操作，支持从用户列表选择执行人 |

### 6.2 关键模块实现

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
  if (userStore.token) {
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

## 7️⃣ 项目启动方式

### 后端启动
```bash
mvn clean install
mvn spring-boot:run
```
访问地址：`http://localhost:8080`

### 前端启动
```bash
npm install
npm run dev
```
访问地址：`http://localhost:5173`

---

## 8️⃣ 项目亮点

- 企业级前后端分离架构
- JWT 登录鉴权与 RBAC 权限控制
- 完整的 CRUD 业务实现（用户/项目/任务）
- 文件上传与任务评论功能
- 前端：Vue 3 + Element Plus + Pinia + TypeScript
- 后端：Spring Boot 3 + MyBatis Plus + Redis + Knife4j

---

## 9️⃣ 后续扩展建议

- WebSocket 实时任务通知
- 邮件或站内消息提醒
- 任务和项目数据 Excel 导出
- Docker 容器化部署