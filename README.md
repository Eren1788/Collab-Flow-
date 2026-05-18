# Collab Flow - 企业级协作管理平台

---

## 1️⃣ 项目简介

Collab Flow 是一个企业级任务协作管理平台，功能类似简化版 Jira/Trello，主要面向团队项目管理。  
该项目主要功能包括：

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

### 3.1 用户表 `user`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像URL |
| role | VARCHAR(20) | 角色：admin/user |
| create_time | DATETIME | 创建时间 |

### 3.2 项目表 `project`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(100) | 项目名称 |
| description | TEXT | 项目描述 |
| creator_id | BIGINT | 创建人ID |
| status | TINYINT | 状态：0未开始 1进行中 2已完成 |
| create_time | DATETIME | 创建时间 |

### 3.3 任务表 `task`

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

### 3.4 评论表 `comment`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 任务ID |
| user_id | BIGINT | 评论人ID |
| content | TEXT | 评论内容 |
| create_time | DATETIME | 评论时间 |

### 3.5 文件表 `file`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 关联任务ID |
| project_id | BIGINT | 关联项目ID |
| name | VARCHAR(100) | 文件名 |
| url | VARCHAR(255) | 文件存储URL |
| uploader_id | BIGINT | 上传人ID |
| upload_time | DATETIME | 上传时间 |

### 3.6 角色权限表

- `role` 表：角色表（admin/user）
- `permission` 表：权限表（user:add, project:edit等）
- `role_permission` 表：角色权限关联
- `user_role` 表：用户角色关联

---

## 4️⃣ 后端接口说明

### 4.1 用户模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /user/login | POST | `{ "username": "xxx", "password": "xxx" }` | `{ "code":200,"data":"token" }` | 用户登录，返回 JWT token |
| /user/register | POST | `{ "username":"xxx","password":"xxx","nickname":"xxx" }` | `{ "code":200 }` | 用户注册 |
| /user/info | GET | Header Authorization | `{ "code":200,"data":{id,username,nickname,role}}` | 获取用户信息 |
| /user/list | GET | Authorization | `{ "code":200,"data":[...] }` | 用户列表 |

### 4.2 项目模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /project/add | POST | `{ "name":"xxx","description":"xxx","status":0 }` | `{ "code":200 }` | 新增项目 |
| /project/list | GET | Authorization | `{ "code":200,"data":[...] }` | 项目列表 |
| /project/update | PUT | `{ "id":1,"name":"xxx" }` | `{ "code":200 }` | 编辑项目 |
| /project/delete/{id} | DELETE | Authorization | `{ "code":200 }` | 删除项目 |

### 4.3 任务模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /task/add | POST | `{ "title":"xxx","content":"xxx","status":0,"executorId":1,"projectId":1 }` | `{ "code":200 }` | 新增任务 |
| /task/list | GET | Authorization | `{ "code":200,"data":[...] }` | 任务列表 |
| /task/update | PUT | `{ "id":1,"title":"xxx" }` | `{ "code":200 }` | 编辑任务 |
| /task/delete/{id} | DELETE | Authorization | `{ "code":200 }` | 删除任务 |

### 4.4 评论模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /task/comment/add | POST | `{ "taskId":1,"content":"xxx" }` | `{ "code":200 }` | 新增评论 |

### 4.5 文件模块

| 接口 | 方法 | 请求参数 | 返回示例 | 说明 |
|------|------|----------|----------|------|
| /file/upload | POST | multipart/form-data file | `{ "code":200,"data":"文件URL" }` | 文件上传 |

---

## 5️⃣ 前端目录结构

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

## 6️⃣ 前端页面说明

### 6.1 登录页 (Login.vue)

- 用户输入用户名和密码
- 调用 `/user/login` 接口
- 成功后存储 token 并跳转首页

### 6.2 首页 (Home.vue)

- 顶部 Header + 左侧 Sidebar + 内容区域
- 使用 `<router-view>` 渲染列表页

### 6.3 用户列表 (UserList.vue)

- 表格显示用户信息
- 调用 `/user/list` 获取数据

### 6.4 项目管理 (ProjectList.vue)

- 表格显示项目
- 支持新增、编辑、删除
- 调用 `/project/add`, `/project/update`, `/project/delete/{id}`

### 6.5 任务管理 (TaskList.vue)

- 表格显示任务
- 支持新增、编辑、删除
- 调用 `/task/add`, `/task/update`, `/task/delete/{id}`
- 可以选择执行人（调用 `/user/list`）

---

## 7️⃣ 启动方式

### 7.1 后端启动

```bash
mvn clean install
mvn spring-boot:run
```

访问接口：http://localhost:8080

### 7.2 前端启动

```bash
npm install
npm run dev
```

访问页面：http://localhost:5173

---

## 8️⃣ 核心代码示例

### 8.1 Axios 封装

```typescript
// src/utils/request.ts
import axios from 'axios'

const request = axios.create({ baseURL: 'http://localhost:8080', timeout: 5000 })

// GET 用户列表
const res = await request.get('/user/list')

// POST 登录
const loginRes = await request.post('/user/login', { username:'admin', password:'123456' })
```

---

## 9️⃣ 项目亮点

- 企业级前后端分离架构
- JWT 登录鉴权、RBAC 权限控制
- CRUD 完整实现
- 文件上传与任务评论
- 前端使用 Element Plus + Pinia + Vue Router
- 后端 MyBatis Plus + Redis + Knife4j 接口文档

---

## 🔟 后续扩展建议

- WebSocket 实时任务通知
- 邮件/消息提醒
- Excel 导出任务和项目
- Docker 部署
