# Collab Flow Web

协同流程管理系统前端项目。

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Vue 3 (Composition API) |
| 语言 | TypeScript |
| 构建工具 | Vite |
| UI 组件库 | Element Plus |
| 图标 | @element-plus/icons-vue |
| 状态管理 | Pinia |
| 路由 | Vue Router |
| HTTP 客户端 | Axios |
| 实时通信 | 原生 WebSocket |

## 本地开发

```bash
# 安装依赖
npm install

# 启动开发服务器 (默认 http://localhost:5173)
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

开发服务器会自动将 `/api` 请求代理到 `http://localhost:8080`。

## 项目结构

详见 [FRONTEND.md](./FRONTEND.md)。

## 功能模块

- **用户管理** — 登录/注册、用户 CRUD、角色管理、头像上传
- **项目管理** — 项目 CRUD、成员管理、进度统计、活动时间线
- **任务管理** — 任务 CRUD、状态流转、优先级筛选、文件附件
- **项目聊天室** — WebSocket 实时消息、图片渲染、未读徽章
- **通知中心** — 实时通知弹窗、已读/未读管理
- **文件管理** — 全局文件列表、上传下载
