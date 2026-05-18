# Collab Flow - 后端文档

---

## 1️⃣ 项目简介

Collab Flow 后端使用 **Spring Boot + MyBatis Plus + MySQL + Redis + JWT + Knife4j**  
提供完整的企业级任务协作管理 API。

功能：

- 用户注册/登录/信息管理
- 项目增删改查
- 任务增删改查、状态管理、指派执行人
- 评论功能
- 文件上传
- 角色权限管理（RBAC）

---

## 2️⃣ 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3 | 核心框架 |
| MyBatis Plus | ORM 框架 |
| Lombok | 简化代码 |
| MySQL 8 | 数据库 |
| Redis | 缓存/Token |
| JWT | 登录鉴权 |
| Knife4j | 自动生成接口文档 |

---

## 3️⃣ 数据库表设计

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
| status | TINYINT | 状态 0未开始 1进行中 2已完成 |
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
| status | TINYINT | 任务状态 0待开始 1进行中 2完成 |
| priority | TINYINT | 优先级 0普通 1紧急 |
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
| url | VARCHAR(255) | 文件URL |
| uploader_id | BIGINT | 上传人ID |
| upload_time | DATETIME | 上传时间 |

### 角色权限表

- `role`：角色表
- `permission`：权限表
- `role_permission`：角色权限关联
- `user_role`：用户角色关联

---

## 4️⃣ 后端接口说明（Apifox 可导入）

### 4.1 用户模块

| 接口 | 方法 | 请求 | 返回示例 | 说明 |
|------|------|------|----------|------|
| /user/login | POST | {username, password} | {code:200,data:"token"} | 登录 |
| /user/register | POST | {username,password,nickname} | {code:200} | 注册 |
| /user/info | GET | Header: Authorization | {code:200,data:{id,username,nickname,role}} | 获取用户信息 |
| /user/list | GET | Header: Authorization | {code:200,data:[...]} | 用户列表 |

### 4.2 项目模块

| 接口 | 方法 | 请求 | 返回示例 | 说明 |
|------|------|------|----------|------|
| /project/add | POST | {name,description,status} | {code:200} | 新增项目 |
| /project/list | GET | Header | {code:200,data:[...]} | 项目列表 |
| /project/update | PUT | {id,name,description,status} | {code:200} | 更新项目 |
| /project/delete/{id} | DELETE | Header | {code:200} | 删除项目 |

### 4.3 任务模块

| 接口 | 方法 | 请求 | 返回示例 | 说明 |
|------|------|------|----------|------|
| /task/add | POST | {title,content,status,executorId,projectId} | {code:200} | 新增任务 |
| /task/list | GET | Header | {code:200,data:[...]} | 任务列表 |
| /task/update | PUT | {id,title,content,status,executorId} | {code:200} | 更新任务 |
| /task/delete/{id} | DELETE | Header | {code:200} | 删除任务 |

### 4.4 评论模块

| 接口 | 方法 | 请求 | 返回示例 | 说明 |
|------|------|------|----------|------|
| /task/comment/add | POST | {taskId, content} | {code:200} | 新增评论 |

### 4.5 文件模块

| 接口 | 方法 | 请求 | 返回示例 | 说明 |
|------|------|------|----------|------|
| /file/upload | POST | multipart/form-data file | {code:200,data:"文件URL"} | 文件上传 |

---

## 5️⃣ 后端启动方式

```bash
mvn clean install
mvn spring-boot:run
```

---

## 6️⃣ 项目亮点

- 企业级架构
- JWT 登录鉴权
- RBAC 权限控制
- CRUD 完整实现
- 文件上传与任务评论
- Knife4j 自动生成接口文档