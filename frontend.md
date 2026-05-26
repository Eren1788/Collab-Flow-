# Collab Flow Web 前端文档

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | 3.5.x |
| 语言 | TypeScript | 6.0 |
| 构建工具 | Vite | 8.x |
| UI 组件库 | Element Plus | 2.14 |
| 图标 | @element-plus/icons-vue | - |
| 状态管理 | Pinia | 3.x |
| 路由 | Vue Router | 5.x |
| HTTP 客户端 | Axios | 1.16 |
| 实时通信 | 原生 WebSocket | - |

---

## 项目结构

```
collab-flow-web/
├── index.html                          # 入口 HTML
├── package.json                        # 依赖与脚本
├── vite.config.ts                      # Vite 配置 (代理 /api → localhost:8080)
├── tsconfig.json                       # TypeScript 根配置
├── tsconfig.app.json                   # 应用 TS 配置
├── tsconfig.node.json                  # 构建工具 TS 配置
│
└── src/
    ├── main.ts                         # 应用入口 (挂载 Vue/Pinia/Router/Element Plus)
    ├── App.vue                         # 根组件 (<router-view />, 恢复 WebSocket)
    ├── style.css                       # 全局样式
    │
    ├── api/                            # API 服务层
    │   ├── user.ts                     #   用户 CRUD、登录、注册、头像上传
    │   ├── project.ts                  #   项目 CRUD、成员管理、列表
    │   ├── task.ts                     #   任务 CRUD、状态更新
    │   ├── comment.ts                  #   评论 CRUD (任务评论 + 项目评论)
    │   ├── notification.ts             #   通知读取、标记已读、未读计数
    │   └── logo.ts                     #   系统 Logo 获取和上传
    │
    ├── store/
    │   └── user.ts                     # Pinia 用户状态 (token, info, permissions, WebSocket)
    │
    ├── router/
    │   └── index.ts                    # 路由配置 + 导航守卫
    │
    ├── utils/
    │   ├── request.ts                  # Axios 实例 (拦截器: token 注入, 错误处理)
    │   └── websocket.ts                # WebSocket 单例 (连接管理, 消息分发, 通知弹窗)
    │
    ├── components/
    │   └── Layout/
    │       ├── Header.vue              #   顶部栏 (Logo, 通知中心, 退出)
    │       ├── Sidebar.vue             #   侧边导航 (权限控制)
    │       └── Footer.vue              #   页脚
    │
    └── views/                          # 页面组件
        ├── Login.vue                   #   登录 + 注册
        ├── Home.vue                    #   主布局 (侧边栏 + Header + <router-view />)
        ├── UserList.vue                #   用户管理
        ├── ProjectList.vue             #   项目列表
        ├── ProjectDetail.vue           #   项目详情 (统计, 任务, 活动时间线)
        ├── ProjectStatistics.vue       #   成员完成情况分析
        ├── TaskList.vue                #   全局任务列表
        ├── TaskDetail.vue              #   任务详情 (文件上传下载)
        ├── CommentList.vue             #   项目聊天室 (WebSocket 实时消息)
        └── FileList.vue                #   全局文件管理
```

---

## 路由表

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | Login.vue | 登录/注册页，未认证入口 |
| `/home` | Home.vue | 主布局壳 (含侧边栏 + Header) |
| `/user/list` | UserList.vue | 用户管理 |
| `/project/list` | ProjectList.vue | 项目列表 |
| `/project/detail/:id` | ProjectDetail.vue | 项目详情 |
| `/project/statistics/:id` | ProjectStatistics.vue | 项目统计 |
| `/tasks` | TaskList.vue | 全局任务列表 |
| `/task/detail/:id` | TaskDetail.vue | 任务详情 |
| `/comments` | CommentList.vue | 项目聊天室 |
| `/files` | FileList.vue | 全局文件管理 |

**导航守卫**: 未携带 token 的访问一律重定向到 `/login`。

---

## 权限模型

双因素权限控制：

### 基于角色 (roleId)

| roleId | 角色 | 权限范围 |
|--------|------|---------|
| 1 | 超级管理员 | 全部权限，可管理所有用户和项目 |
| 2 | 项目经理 | 管理自己的项目 |
| 其他 | 普通成员 | 仅查看被分配的任务和参与的项目 |

### 基于权限字符串

`info.permissions` 数组中包含权限标识（如 `user:list`、`project:list`、`task:list`），侧边栏菜单项通过 `hasPermission()` getter 控制显隐。

---

## 状态管理 (Pinia)

**唯一 Store**: `useUserStore` (`src/store/user.ts`)

| 字段/方法 | 说明 |
|-----------|------|
| `token` | JWT 令牌，同步到 localStorage |
| `info` | 用户信息对象 (id, username, roleId, permissions 等) |
| `hasPermission(p)` | Getter，检查 info.permissions 是否包含权限 p |
| `setToken(t)` | 设置 token 并写入 localStorage |
| `setUserInfo(info)` | 设置用户信息并写入 localStorage |
| `connectWebSocket()` | 建立 WebSocket 连接 |
| `logout()` | 清除状态、localStorage、断开 WebSocket |

---

## API 通信

### Axios 实例 (`src/utils/request.ts`)

- `baseURL`: `/api`
- `timeout`: 10000ms
- **请求拦截器**: 自动从 Pinia store 附加 `Authorization: Bearer <token>`
- **响应拦截器**: 检查 `code !== 200` 时弹出 ElMessage 错误提示

### 响应约定

```typescript
interface ApiResponse<T> {
  code: number    // 200 = 成功
  message: string
  data: T
}
```

### API 模块一览

| 模块 | 主要接口 |
|------|---------|
| `user.ts` | login, register, getList, getById, update, delete, updateAvatar |
| `project.ts` | getPage, getById, create, update, delete, getMembers, addMember |
| `task.ts` | getPage, getById, create, update, delete, updateStatus |
| `comment.ts` | getList (任务评论), getProjectComments, addTaskComment, addProjectComment |
| `notification.ts` | getList, markAsRead, markAllAsRead, delete, getUnreadCount |
| `logo.ts` | getLogo, uploadLogo |

---

## WebSocket 实时通信

### 连接

`WebSocketService` 单例 (`src/utils/websocket.ts`)，全局唯一连接：

```
ws://localhost:8080/ws/notification?userId=<当前用户ID>
```

### 消息分发

基于监听器订阅模式：

- 组件通过 `addMessageListener(callback)` 注册监听
- 全局通知 (TASK_ASSIGN, TASK_STATUS, TASK_CREATE 等) 自动触发 ElNotification 弹窗
- 聊天消息 (NEW_COMMENT, NEW_PROJECT_COMMENT) 跳过全局弹窗，仅通过监听器分发到聊天组件

### 生命周期

| 时机 | 操作 |
|------|------|
| 登录成功 | `connectWebSocket()` |
| 页面刷新 | `App.vue` onMounted 检测 token 后重连 |
| 退出登录 | `logout()` → `disconnect()` |

---

## 通知类型

| 类型标识 | 含义 |
|----------|------|
| `TASK_ASSIGN` | 任务分配 |
| `TASK_COMMENT` | 任务评论 |
| `TASK_FILE` | 任务文件 |
| `TASK_CREATE` | 任务创建 |
| `TASK_STATUS` | 任务状态变更 |
| `QUESTION` | 项目提问 |
| `REPLY` | 项目回复 |

点击通知跳转到对应的任务详情页。

---

## 业务功能

### 用户管理
- 超级管理员看到全部用户；普通用户看到除自己外的其他用户
- 功能：搜索、分页、编辑、删除、启用/禁用、头像上传
- 个人信息卡片展示当前用户详情

### 项目管理
- 列表：按名称/状态搜索、分页、进度条
- 详情：统计仪表板 (总数/已完成/进行中/已逾期)、成员管理、任务内嵌 CRUD、活动时间线
- 统计：按成员展示完成情况、任务进度、上传文件

### 任务管理
- 全局列表：统计卡片 (可点击筛选)、关键词/状态/优先级搜索、分页
- 任务详情：完整元数据、文件上传下载 (Blob 模式，Content-Disposition 文件名解析)
- 非管理员仅看到分配给自己的任务

### 项目聊天室
- 左侧项目列表 (带未读徽章)，右侧消息气泡
- 支持图片上传 (Markdown 语法 `![](url)` 渲染)
- WebSocket 实时收发 `NEW_PROJECT_COMMENT` 消息

### 文件管理
- 全局文件列表：上传、下载 (`window.open` 直连后端)、删除
- 任务附件：通过 `<el-upload>` 上传，Blob 下载并解析文件名
