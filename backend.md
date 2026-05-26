# Collab Flow - 后端开发文档（完整版）

---

## 1. 项目简介

Collab Flow 是一个**企业级任务协作管理平台**后端系统，功能类似简化版 Jira/Trello。

**核心技术栈：**

| 技术 | 版本/说明 |
|------|----------|
| Java | 17 |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.5 |
| MySQL | 8.0 |
| Redis | 会话管理 + Token 缓存 |
| JWT (jjwt) | 0.11.5 |
| Knife4j | 4.5.0 (API 文档) |
| WebSocket | Spring WebSocket (实时通知) |
| AOP | Spring AOP (操作日志) |
| Lombok | 代码简化 |
| Hutool | 5.8.26 (工具类) |
| Fastjson2 | 2.0.52 |
| Maven | 构建工具 |

**系统功能：**
- 用户注册/登录（JWT + Redis 鉴权）
- RBAC 权限控制（5 张权限相关表）
- 项目 CRUD + 成员管理 + 任务统计
- 任务 CRUD + 多执行人 + 状态流转 + 问答
- 任务评论（多级回复）+ 项目聊天室
- 文件上传/下载/删除
- 实时 WebSocket 消息推送
- 站内通知系统
- AOP 操作日志记录
- 分页查询 + 多条件筛选

---

## 2. 项目目录结构

```
src/main/java/com/collab/
├── CollabFlowApplication.java          # 启动类
├── common/
│   ├── annotation/
│   │   ├── OperationLogAnnotation.java  # AOP日志注解
│   │   └── RequirePermission.java       # 权限注解
│   ├── aspect/
│   │   └── OperationLogAspect.java      # AOP操作日志切面
│   ├── constant/
│   │   └── RedisConstant.java           # Redis Key 常量
│   ├── exception/
│   │   ├── BusinessException.java       # 业务异常
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   ├── interceptor/
│   │   ├── JwtInterceptor.java          # JWT 登录拦截器
│   │   └── PermissionInterceptor.java   # RBAC 权限拦截器
│   ├── result/
│   │   └── Result.java                  # 统一响应封装
│   └── utils/
│       ├── JwtUtils.java               # JWT 工具类
│       └── LoginUserContext.java       # ThreadLocal 用户上下文
├── config/
│   ├── Knife4jConfig.java              # API 文档配置
│   ├── MybatisPlusConfig.java          # MyBatis-Plus 分页插件
│   ├── RedisConfig.java                # Redis 序列化配置
│   ├── WebMvcConfig.java               # 拦截器/CORS/静态资源
│   └── WebSocketConfig.java            # WebSocket 端点注册
├── controller/                          # 控制器层 (10 个)
│   ├── UserController.java
│   ├── ProjectController.java
│   ├── TaskController.java
│   ├── CommentController.java
│   ├── FileController.java
│   ├── NotificationController.java
│   ├── ProjectActivityController.java
│   ├── RoleController.java
│   ├── LogoController.java
│   └── TestController.java
├── dto/                                 # 请求体 (9 个)
│   ├── LoginDTO.java
│   ├── UserRegisterDTO.java
│   ├── UserUpdateDTO.java
│   ├── ProjectDTO.java
│   ├── ProjectMemberDTO.java
│   ├── TaskDTO.java
│   ├── TaskStatusDTO.java
│   ├── TaskAssignDTO.java
│   └── CommentDTO.java
├── entity/                              # 数据库实体 (15 个)
│   ├── User.java
│   ├── Project.java
│   ├── ProjectMember.java
│   ├── Task.java
│   ├── TaskExecutor.java
│   ├── TaskReadStatus.java
│   ├── Comment.java
│   ├── FileInfo.java
│   ├── Notification.java
│   ├── OperationLog.java
│   ├── ProjectActivity.java
│   ├── Role.java
│   ├── Permission.java
│   ├── UserRole.java
│   └── RolePermission.java
├── mapper/                              # MyBatis Mapper (15 个)
│   ├── UserMapper.java
│   ├── ProjectMapper.java
│   ├── ProjectMemberMapper.java
│   ├── TaskMapper.java
│   ├── TaskExecutorMapper.java
│   ├── TaskReadStatusMapper.java
│   ├── CommentMapper.java
│   ├── FileInfoMapper.java
│   ├── NotificationMapper.java
│   ├── OperationLogMapper.java
│   ├── ProjectActivityMapper.java
│   ├── RoleMapper.java
│   ├── PermissionMapper.java
│   ├── UserRoleMapper.java
│   └── RolePermissionMapper.java
├── service/                             # 服务接口 (10 个)
│   ├── UserService.java
│   ├── ProjectService.java
│   ├── ProjectMemberService.java
│   ├── TaskService.java
│   ├── CommentService.java
│   ├── FileService.java
│   ├── NotificationService.java
│   ├── ProjectActivityService.java
│   ├── RoleService.java
│   └── LogoService.java
├── service/impl/                        # 服务实现 (10 个)
│   ├── UserServiceImpl.java
│   ├── ProjectServiceImpl.java
│   ├── ProjectMemberServiceImpl.java
│   ├── TaskServiceImpl.java
│   ├── CommentServiceImpl.java
│   ├── FileServiceImpl.java
│   ├── NotificationServiceImpl.java
│   ├── ProjectActivityServiceImpl.java
│   ├── RoleServiceImpl.java
│   └── LogoServiceImpl.java
├── vo/                                  # 视图对象 (6 个)
│   ├── UserVO.java
│   ├── ProjectVO.java
│   ├── TaskVO.java
│   ├── CommentVO.java
│   ├── NotificationVO.java
│   └── RoleVO.java
└── websocket/
    ├── NotificationMessage.java         # 消息结构体
    └── NotificationWebSocketHandler.java # WS 处理器

src/main/resources/
├── application.yml
└── com/collab/mapper/
    ├── UserMapper.xml
    ├── RoleMapper.xml
    ├── PermissionMapper.xml
    └── UserRoleMapper.xml
```

---

## 3. 数据库设计（15 张表）

### 3.1 用户表 `user`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | 密码（明文存储） |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| status | TINYINT | 状态：1=启用 0=禁用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 3.2 项目表 `project`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(100) | 项目名称 |
| description | TEXT | 项目描述 |
| creator_id | BIGINT | 创建人 ID |
| status | TINYINT | 0=未开始 1=进行中 2=已完成 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 截止时间 |
| create_time | DATETIME | 创建时间 |

### 3.3 项目成员表 `project_member`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 项目 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR(20) | 项目内角色：admin/member |
| join_time | DATETIME | 加入时间 |

### 3.4 任务表 `task`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 所属项目 ID |
| title | VARCHAR(100) | 任务标题 |
| content | TEXT | 任务内容 |
| creator_id | BIGINT | 创建人 ID |
| status | TINYINT | 0=待开始 1=进行中 2=已完成 |
| priority | TINYINT | 0=普通 1=紧急 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 截止时间 |
| create_time | DATETIME | 创建时间 |

### 3.5 任务执行人表 `task_executor`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 任务 ID |
| user_id | BIGINT | 执行人用户 ID |

> 一个任务可指派多个执行人，通过此表实现多对多关系。

### 3.6 任务已读状态表 `task_read_status`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户 ID |
| task_id | BIGINT | 任务 ID |
| last_read_time | DATETIME | 最后阅读时间 |

> 用于计算每个任务的未读评论数。

### 3.7 评论表 `comment`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 任务 ID（任务评论时使用） |
| project_id | BIGINT | 项目 ID（项目聊天室时使用） |
| user_id | BIGINT | 评论人 ID |
| content | TEXT | 评论内容 |
| parent_id | BIGINT | 父评论 ID，0=顶层评论 |
| create_time | DATETIME | 创建时间 |

### 3.8 文件表 `file_info`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| task_id | BIGINT | 关联任务 ID |
| project_id | BIGINT | 关联项目 ID |
| name | VARCHAR(255) | 原始文件名 |
| url | VARCHAR(500) | 存储文件名（UUID） |
| file_size | BIGINT | 文件大小（字节） |
| file_type | VARCHAR(50) | MIME 类型 |
| uploader_id | BIGINT | 上传人 ID |
| upload_time | DATETIME | 上传时间 |

### 3.9 通知表 `notification`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| receiver_id | BIGINT | 接收人 ID |
| sender_id | BIGINT | 发送人 ID |
| type | VARCHAR(50) | 类型：TASK_CREATE/TASK_STATUS/TASK_ASSIGN/COMMENT/PROJECT_COMMENT/QUESTION/REPLY |
| content | VARCHAR(500) | 通知内容 |
| business_id | BIGINT | 业务 ID（任务 ID 或项目 ID） |
| is_read | TINYINT | 0=未读 1=已读 |
| create_time | DATETIME | 创建时间 |

### 3.10 操作日志表 `operation_log`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 操作用户 ID |
| username | VARCHAR(50) | 操作用户名 |
| operation | VARCHAR(100) | 操作名称（如"新增项目"、"删除任务"） |
| method | VARCHAR(10) | HTTP 方法 |
| request_uri | VARCHAR(255) | 请求 URI |
| request_params | TEXT | 请求参数（JSON） |
| ip | VARCHAR(50) | 客户端 IP |
| status | TINYINT | 1=成功 0=失败 |
| error_msg | VARCHAR(2000) | 错误信息 |
| create_time | DATETIME | 创建时间 |

### 3.11 项目动态表 `project_activity`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 项目 ID |
| user_id | BIGINT | 用户 ID |
| content | VARCHAR(500) | 动态内容 |
| type | VARCHAR(50) | 类型：PROJECT_CREATE/TASK_CREATE/COMMENT/PROJECT_COMMENT/MEMBER_ADD |
| create_time | DATETIME | 创建时间 |

### 3.12 角色表 `role`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增。1=超级管理员 2=项目经理 3=普通成员 |
| role_name | VARCHAR(50) | 角色名称 |
| role_code | VARCHAR(50) | 角色编码：SUPER_ADMIN/PROJECT_MANAGER/MEMBER |
| description | VARCHAR(255) | 描述 |
| create_time | DATETIME | 创建时间 |

### 3.13 权限表 `permission`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| permission_name | VARCHAR(50) | 权限名称 |
| permission_code | VARCHAR(50) | 权限编码（如 user:list, task:add） |
| type | VARCHAR(20) | 类型 |
| path | VARCHAR(255) | 路径 |

### 3.14 用户-角色关联表 `user_role`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户 ID |
| role_id | BIGINT | 角色 ID |

### 3.15 角色-权限关联表 `role_permission`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| role_id | BIGINT | 角色 ID |
| permission_id | BIGINT | 权限 ID |

---

## 4. 鉴权体系

### 4.1 JWT + Redis 登录鉴权

**JwtInterceptor**（登录拦截器）执行流程：

```
请求 → 提取 Authorization: Bearer <token>
    → 校验 Bearer 格式
    → JwtUtils.parseToken() 解析 JWT
    → 校验过期时间
    → 提取 userId
    → Redis 校验 key: login:token:{userId}
    → 自动续期（剩余 < 1天时续 7 天）
    → LoginUserContext.setUserId(userId) 存入 ThreadLocal
    → 请求结束 afterCompletion 清除 ThreadLocal
```

**放行白名单**（无需登录）：
- `/user/login`、`/user/register`
- `/logo/url`、`/logo/image`
- `/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**`
- `/error`、`/uploads/**`

**JWT 参数：**
- 密钥：Base64 编码，HS256 签名
- 有效期：7 天
- 载荷：userId, username

**Redis 存储：**
- Key: `login:token:{userId}`
- Value: userId
- 过期时间：7 天
- 退出登录：直接删除 Redis key

### 4.2 RBAC 权限控制

**PermissionInterceptor**（权限拦截器）执行流程：

```
请求 → 获取方法上的 @RequirePermission("code")
    → 无注解 → 直接放行
    → 有注解 → 从 ThreadLocal 获取 userId
    → 查 user_role 表获取用户角色
    → 查 role_permission 表获取角色权限
    → 查 permission 表获取权限编码列表
    → 判断是否包含所需权限编码
    → 不包含 → BusinessException("权限不足")
```

**权限注解使用：**
```java
@RequirePermission("user:list")   // 需要 user:list 权限
@RequirePermission("task:add")    // 需要 task:add 权限
```

### 4.3 权限查询 SQL（联合查询）

通过 `PermissionMapper.xml` 中的 `getPermissionCodesByUserId` 一次性查询用户的全部权限编码：
```sql
SELECT DISTINCT p.permission_code
FROM user_role ur
JOIN role_permission rp ON ur.role_id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE ur.user_id = #{userId}
```

---

## 5. 统一返回结构

```java
public class Result<T> {
    private Integer code;    // 200=成功 500=失败
    private String message;  // 提示信息
    private T data;          // 数据
}
```

**成功示例：**
```json
{ "code": 200, "message": "success", "data": { ... } }
```

**分页示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": [ ... ]
  }
}
```

---

## 6. API 接口文档

### 6.1 用户模块 `/user`

| 接口 | 方法 | 认证 | 权限 | 说明 |
|------|------|------|------|------|
| `/user/login` | POST | 否 | - | 用户登录，返回 token + userInfo |
| `/user/register` | POST | 否 | - | 用户注册，同时绑定角色 |
| `/user/logout` | POST | 是 | - | 退出登录，清除 Redis 会话 |
| `/user/info` | GET | 是 | - | 获取当前用户信息（含角色+权限列表） |
| `/user/list` | GET | 是 | `user:list` | 获取所有用户列表（含角色名） |
| `/user/page` | GET | 是 | - | 用户分页（pageNum, pageSize, keyword） |
| `/user/detail/{id}` | GET | 是 | - | 用户详情 |
| `/user/update` | PUT | 是 | - | 修改用户（管理员可改角色/状态） |
| `/user/delete/{id}` | DELETE | 是 | - | 删除用户 |
| `/user/avatar` | POST | 是 | - | 上传头像（5MB限制，存为 userId.png） |

**登录请求体 LoginDTO：**
```json
{ "username": "admin", "password": "123456" }
```

**注册请求体 UserRegisterDTO：**
```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "roleId": 3
}
```

**更新请求体 UserUpdateDTO：**
```json
{
  "id": 1,
  "nickname": "新昵称",
  "email": "new@example.com",
  "phone": "13900139000",
  "status": 1,
  "roleId": 2
}
```

> 权限规则：普通用户只能修改自己的信息；管理员才能修改状态和角色。

### 6.2 项目模块 `/project`

| 接口 | 方法 | 认证 | AOP日志 | 说明 |
|------|------|------|---------|------|
| `/project/add` | POST | 是 | "新增项目" | 创建项目，自动添加创建人为 admin 成员 |
| `/project/list` | GET | 是 | - | 所有项目列表 |
| `/project/page` | GET | 是 | - | 项目分页（pageNum, pageSize, keyword, status） |
| `/project/detail/{id}` | GET | 是 | - | 项目详情（含任务数量） |
| `/project/update` | PUT | 是 | - | 修改项目 |
| `/project/delete/{id}` | DELETE | 是 | - | 删除项目（级联删除成员+任务） |
| `/project/member/add` | POST | 是 | - | 添加项目成员，重复添加会报错 |
| `/project/member/list/{projectId}` | GET | 是 | - | 项目成员列表（含系统角色名） |
| `/project/member/delete/{id}` | DELETE | 是 | - | 删除项目成员 |
| `/project/statistics/{projectId}` | GET | 是 | - | 成员任务统计（每人 total/completed/progress/files） |

> 分页查询：非管理员只能看到自己参与的项目。

**请求体 ProjectDTO：**
```json
{ "name": "项目名", "description": "描述", "status": 0 }
```

### 6.3 任务模块 `/task`

| 接口 | 方法 | 认证 | 权限 | AOP日志 | 说明 |
|------|------|------|------|---------|------|
| `/task/add` | POST | 是 | `task:add` | - | 创建任务（多执行人），WS通知执行人 |
| `/task/list` | GET | 是 | - | - | 任务列表 |
| `/task/page` | GET | 是 | - | - | 分页（projectId/status/executorId/priority/keyword） |
| `/task/detail/{id}` | GET | 是 | - | - | 任务详情（含执行人列表+未读评论数） |
| `/task/update` | PUT | 是 | - | - | 修改任务（先删后插执行人） |
| `/task/delete/{id}` | DELETE | 是 | - | "删除任务" | 删除任务（级联删除执行人关联） |
| `/task/status` | PUT | 是 | - | - | 修改任务状态，WS通知创建人 |
| `/task/assign` | PUT | 是 | - | - | 指派单个执行人，WS通知 |
| `/task/statistics` | GET | 是 | - | - | 全局统计（total/todo/doing/done） |
| `/task/question` | POST | 是 | - | - | 向项目经理提问，WS通知 |
| `/task/reply` | POST | 是 | - | - | 回复疑问（仅管理员/项目经理），WS通知 |
| `/task/read/{taskId}` | PUT | 是 | - | - | 标记任务已读（记录当前时间） |

**请求体 TaskDTO：**
```json
{
  "projectId": 1,
  "title": "任务标题",
  "content": "任务内容",
  "executorIds": [2, 3],
  "priority": 0,
  "status": 0
}
```

**未读评论计算逻辑：**
- `task_read_status` 表记录每个用户对每个任务的最后阅读时间
- 分页查询时，统计 `create_time > last_read_time` 的评论数
- 首次查看（无记录）时，以 1970-01-01 为基准，所有评论都算未读

### 6.4 评论模块 `/comment`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/comment/add` | POST | 任务评论（支持 parentId 多级回复） |
| `/comment/list/{taskId}` | GET | 任务评论列表（按时间升序） |
| `/comment/delete/{id}` | DELETE | 删除评论（级联删除子评论） |
| `/comment/project/add` | POST | 项目聊天室发言 |
| `/comment/project/list/{projectId}` | GET | 项目聊天室消息列表 |

**WebSocket 通知机制（任务评论）：**
1. 通知任务创建人（如非评论人自己）
2. 通知所有任务执行人（如非评论人自己）
3. 推送实时评论数据（NEW_COMMENT）到前端在线用户

**WebSocket 通知机制（项目评论）：**
1. 通知所有项目成员（如非发言者自己）
2. 通知内容截断至 30 字

**请求体 CommentDTO：**
```json
{
  "taskId": 1,
  "projectId": null,
  "content": "评论内容",
  "parentId": 0
}
```

### 6.5 文件模块 `/file`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/file/upload` | POST | 上传文件（multipart/form-data） |
| `/file/list` | GET | 文件列表（按 taskId 或 projectId 过滤） |
| `/file/delete/{id}` | DELETE | 删除文件（物理文件 + 数据库记录） |
| `/file/download/{id}` | GET | 下载文件（Content-Disposition: attachment） |

**上传参数：**
| 参数 | 类型 | 必填 |
|------|------|------|
| file | MultipartFile | 是 |
| taskId | Long | 否 |
| projectId | Long | 否 |

**文件存储：**
- 物理路径：`E:/collabflow/upload/{UUID}.{ext}`
- 访问 URL：`/uploads/{UUID}.{ext}`（通过 WebMvcConfig 静态资源映射）
- 文件名：UUID 重命名，保留原始扩展名

### 6.6 通知模块 `/notification`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/notification/my` | GET | 我的通知列表（按时间倒序） |
| `/notification/unread/count` | GET | 未读通知数量 |
| `/notification/read/{id}` | PUT | 标记单条已读 |
| `/notification/read/all` | PUT | 全部标记已读 |
| `/notification/delete` | DELETE | 批量删除（仅能删除自己的） |

**通知类型枚举：**
| 类型 | 触发场景 |
|------|---------|
| TASK_CREATE | 创建任务时通知执行人 |
| TASK_STATUS | 任务状态变更时通知创建人 |
| TASK_ASSIGN | 指派任务时通知被指派人 |
| COMMENT | 任务评论时通知创建人+所有执行人 |
| PROJECT_COMMENT | 项目聊天室发言时通知所有项目成员 |
| QUESTION | 成员向项目经理提问 |
| REPLY | 项目经理/管理员回复疑问 |

### 6.7 其他模块

**角色模块 `/role`**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/role/list` | GET | 角色职位列表 |

**Logo 管理 `/logo`**

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/logo/url` | GET | 否 | 获取系统 Logo URL |
| `/logo/image` | GET | 否 | 直接返回 Logo 图片字节流 |
| `/logo/upload` | POST | 是（管理员） | 上传/更新 Logo |

**项目动态 `/project/activity`**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/project/activity/list/{projectId}` | GET | 项目动态时间线 |

**健康检查 `/test`**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/test/` | GET | 服务健康检查 |

---

## 7. 状态与枚举定义

### 7.1 项目状态

| 值 | 含义 |
|----|------|
| 0 | 未开始 |
| 1 | 进行中 |
| 2 | 已完成 |

### 7.2 任务状态

| 值 | 含义 |
|----|------|
| 0 | 待开始 |
| 1 | 进行中 |
| 2 | 已完成 |

### 7.3 任务优先级

| 值 | 含义 |
|----|------|
| 0 | 普通 |
| 1 | 紧急 |

### 7.4 用户状态

| 值 | 含义 |
|----|------|
| 0 | 禁用 |
| 1 | 启用 |

### 7.5 角色 ID（预设）

| ID | 角色编码 | 名称 |
|----|---------|------|
| 1 | SUPER_ADMIN | 超级管理员 |
| 2 | PROJECT_MANAGER | 项目经理 |
| 3 | MEMBER | 普通成员 |

---

## 8. WebSocket 实时通信

### 8.1 连接方式

```
ws://localhost:8080/ws/notification?userId={userId}
```

### 8.2 消息结构

```java
public class NotificationMessage {
    private String type;       // TASK_CREATE/COMMENT/QUESTION/REPLY 等
    private String content;    // 通知内容（JSON 字符串）
    private Long businessId;   // 业务 ID
    private Long timestamp;    // 毫秒时间戳
}
```

### 8.3 核心实现

- **NotificationWebSocketHandler**: 继承 TextWebSocketHandler
- **会话管理**: `ConcurrentHashMap<Long, WebSocketSession>` 存储在线用户
- **连接建立**: 从 URL 参数提取 userId，存入 Map
- **连接关闭**: 从 Map 中移除
- **消息发送**: 静态方法 `sendMessage(userId, message)`，Service 层直接调用
- **离线处理**: 用户不在线时仅记录日志，不阻塞业务

### 8.4 使用场景

| 场景 | 触发时机 | 通知对象 |
|------|---------|---------|
| 任务创建 | addTask() | 所有执行人 |
| 状态变更 | updateStatus() | 任务创建人 |
| 任务指派 | assignTask() | 被指派人 |
| 任务评论 | addComment() | 创建人 + 所有执行人（排除自己） |
| 项目评论 | addProjectComment() | 所有项目成员（排除自己） |
| 提问 | sendQuestion() | 项目经理 |
| 回复 | sendReply() | 提问的成员 |

---

## 9. AOP 操作日志

### 9.1 注解使用

```java
@OperationLogAnnotation("新增项目")
@PostMapping("/add")
public Result<Void> add(@RequestBody ProjectDTO dto) { ... }

@OperationLogAnnotation("删除任务")
@DeleteMapping("/delete/{id}")
public Result<Void> delete(@PathVariable Long id) { ... }
```

### 9.2 记录内容

| 字段 | 来源 |
|------|------|
| userId | LoginUserContext.getUserId() |
| username | 查 user 表 |
| operation | 注解 value |
| method | request.getMethod() |
| requestUri | request.getRequestURI() |
| requestParams | joinPoint.getArgs() 序列化为 JSON |
| ip | request.getRemoteAddr() |
| status | 1=成功 0=异常 |
| errorMsg | 异常 message |

### 9.3 当前使用位置

- `ProjectController.add()` — "新增项目"
- `TaskController.delete()` — "删除任务"

---

## 10. 文件上传配置

```yaml
file:
  upload-path: E:/collabflow/upload

spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB
```

**目录结构：**
```
E:/collabflow/upload/
├── {uuid}.pdf          # 普通文件（UUID 命名）
├── avatar/
│   └── {userId}.png    # 用户头像
└── logo/
    └── system-logo.png  # 系统 Logo
```

**静态资源映射**（WebMvcConfig）：
```java
registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:E:/collabflow/upload/");
```

---

## 11. 配置详解

### 11.1 application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/collab_flow?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: root
    password: 155215
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: 192.168.100.128
      port: 6380
      password: collab123
      database: 0
      connect-timeout: 5000
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

mybatis-plus:
  mapper-locations: classpath*:/com/collab/mapper/**/*.xml
  type-aliases-package: com.collab.entity
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true

knife4j:
  enable: true
  setting:
    language: zh_cn

file:
  upload-path: E:/collabflow/upload
```

### 11.2 配置类一览

| 配置类 | 作用 |
|--------|------|
| WebMvcConfig | 注册 JWT + 权限拦截器；CORS 全放行；静态资源映射 |
| MybatisPlusConfig | MyBatis-Plus 分页插件 |
| RedisConfig | RedisTemplate<String, Object> JSON 序列化 |
| Knife4jConfig | Knife4j/Swagger 文档配置 |
| WebSocketConfig | WebSocket 端点注册 |

---

## 12. 全局异常处理

`GlobalExceptionHandler` 统一处理：

- **BusinessException**: 返回 `Result.error(500, message)`
- **JwtException**: 返回 `Result.error(401, "请先登录")`
- **Exception**: 兜底处理，返回 `Result.error(500, "系统异常")`

---

## 13. 分页查询规范

所有分页接口统一参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | Integer | 1 | 当前页 |
| pageSize | Integer | 10 | 每页条数 |

返回结构：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": [ ... ],
    "pages": 10,
    "current": 1,
    "size": 10
  }
}
```

---

## 14. 项目启动

```bash
# 1. 确保 MySQL 和 Redis 已启动
# 2. 创建数据库
# CREATE DATABASE collab_flow DEFAULT CHARACTER SET utf8mb4;

# 3. 启动项目
mvn clean install
mvn spring-boot:run

# 4. 访问 API 文档
# http://localhost:8080/doc.html
```

---

## 15. 项目完成度

| 模块 | 完成度 | 备注 |
|------|--------|------|
| 登录鉴权（JWT+Redis） | 100% | 含自动续期、退出登录 |
| 用户模块 | 100% | CRUD + 头像 + 角色绑定 |
| 项目模块 | 100% | CRUD + 成员 + 统计 + 动态 |
| 任务模块 | 100% | CRUD + 多执行人 + 状态 + 问答 + 已读 |
| 评论模块 | 100% | 任务评论 + 项目聊天室 + WS 实时推送 |
| 文件模块 | 100% | 上传 + 下载 + 删除 |
| 权限模块（RBAC） | 100% | 五表 + 注解拦截 |
| 消息通知 | 100% | 站内通知 + WebSocket |
| 操作日志（AOP） | 100% | 环绕通知记录 |
| Logo 管理 | 100% | 上传 + 公开获取 |

**整体完成度：约 100%**

---

## 16. 架构亮点

- **JWT + Redis 双重校验**：JWT 保证无状态，Redis 支持主动踢人（退出登录）
- **RBAC 五表权限**：用户→角色→权限，灵活扩展
- **多执行人设计**：通过 `task_executor` 关联表实现一对多
- **已读/未读**：`task_read_status` 表记录阅读时间，精确计算未读评论数
- **WebSocket 实时推送**：任务变更即时通知 + 评论实时聊天
- **AOP 日志**：非侵入式操作审计
- **级联删除**：项目→成员+任务，任务→执行人关联，评论→子评论
- **权限隔离**：普通用户只能看自己参与的项目
- **ThreadLocal 上下文**：Service 层无侵入获取当前用户

---

## 17. 后续扩展建议

- 密码加密（BCrypt）
- Redis Token 续期优化
- 邮件通知
- Docker 容器化部署
- MinIO/NFS 对象存储
- Nginx 反向代理
- 数据导出（Excel）
- 消息队列（RabbitMQ/Kafka）
- 微服务拆分
- CI/CD 流水线