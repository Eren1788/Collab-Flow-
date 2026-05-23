# Collab Flow - 后端开发文档（完整版）

# 1️⃣ 项目简介

Collab Flow 是一个基于：

* Spring Boot 3
* MyBatis Plus
* MySQL 8
* Redis
* JWT
* Knife4j
* WebSocket
* AOP

开发的企业级智能任务协作系统。

系统支持：

* 用户登录注册
* JWT + Redis 鉴权
* RBAC 权限控制（角色-权限）
* 项目管理
* 项目成员管理
* 任务协作
* 评论系统
* 文件上传
* 分页查询
* 条件筛选
* 任务统计
* 实时消息通知（WebSocket）
* 操作日志记录（AOP）

适用于：

* Java Web 课程设计
* 毕业设计
* Spring Boot 全栈项目
* 企业级 CRUD 项目练习

---

# 2️⃣ 技术栈

| 技术            | 说明      |
| ------------- | ------- |
| Spring Boot 3 | 核心框架    |
| MyBatis Plus  | ORM框架   |
| MySQL 8       | 数据库     |
| Redis         | Token缓存/会话管理 |
| JWT           | 登录鉴权    |
| Lombok        | 简化代码    |
| Knife4j       | 接口文档    |
| Maven         | 项目管理    |
| WebSocket     | 实时消息推送  |
| AOP           | 操作日志切面  |
| Hutool        | 通用工具类库  |
| Fastjson2     | JSON序列化  |

---

# 3️⃣ 推荐项目目录结构
```
text
com.collab
├── common
│   ├── annotation (自定义注解: @RequirePermission, @OperationLogAnnotation)
│   ├── aspect (AOP切面: OperationLogAspect)
│   ├── constant (常量: RedisConstant)
│   ├── exception (异常: BusinessException, GlobalExceptionHandler)
│   ├── interceptor (拦截器: JwtInterceptor, PermissionInterceptor)
│   ├── result (统一返回: Result<T>)
│   └── utils (工具类: JwtUtils, LoginUserContext)
├── config (配置类: WebMvcConfig, MybatisPlusConfig, RedisConfig, Knife4jConfig, WebSocketConfig)
├── controller (控制器: User/Project/Task/Comment/File/NotificationController)
├── dto (数据传输对象: LoginDTO, UserRegisterDTO, ProjectDTO, TaskDTO等)
├── entity (实体类: User, Project, Task, Comment, FileInfo, Notification, OperationLog等)
├── mapper (Mapper接口: 继承BaseMapper<Entity>)
├── service (Service接口)
├── service.impl (Service实现: 继承ServiceImpl<Mapper, Entity>)
├── vo (视图对象: UserVO, ProjectVO, TaskVO等)
└── websocket (WebSocket处理器: NotificationWebSocketHandler)
```

---

# 4️⃣ 数据库设计（最终版）

# 4.1 用户表 user

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'user',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

# 4.2 项目表 project

```sql
CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    creator_id BIGINT,
    status TINYINT DEFAULT 0,
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

# 4.3 项目成员表 project_member

```sql
CREATE TABLE project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'member',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

# 4.4 任务表 task

```sql
CREATE TABLE task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    creator_id BIGINT,
    executor_id BIGINT,
    status TINYINT DEFAULT 0,
    priority TINYINT DEFAULT 0,
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

# 4.5 评论表 comment

```sql
CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    parent_id BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

# 4.6 文件表 file_info

```sql
CREATE TABLE file_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT,
    project_id BIGINT,
    name VARCHAR(255),
    url VARCHAR(500),
    file_size BIGINT,
    file_type VARCHAR(50),
    uploader_id BIGINT,
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

# 5️⃣ JWT 与 Redis 鉴权规范

所有需要登录的接口，请求头需携带：

```http
Authorization: Bearer <token>
```

**鉴权流程：**
1. **JwtInterceptor**: 验证 Token 合法性、有效期，并从 Redis 中校验会话是否存在。
2. **LoginUserContext**: 使用 `ThreadLocal` 存储当前登录用户 ID，方便在 Service 层获取。
3. **PermissionInterceptor**: 根据 `@RequirePermission` 注解进行 RBAC 权限校验。

---

# 5.1 RBAC 权限控制

系统采用 **用户-角色-权限** 模型：

* **@RequirePermission("code")**: 标注在 Controller 方法上，指定所需权限编码。
* **PermissionInterceptor**: 拦截请求，查询用户关联的角色及角色拥有的权限，若不匹配则抛出 `BusinessException("权限不足")`。

---

# 5.2 操作日志（AOP）

通过 `@OperationLogAnnotation("操作名称")` 自动记录用户行为：

* **记录内容**: 操作用户、IP地址、请求方法、URI、参数、耗时、执行状态。
* **实现方式**: 使用 Spring AOP `@Around` 环绕通知，在方法执行前后记录日志并持久化到 `operation_log` 表。

---

# 6️⃣ 统一返回结构

# Result.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success(T data){
        return new Result<>(200,"success",data);
    }

    public static <T> Result<T> success(){
        return new Result<>(200,"success",null);
    }

    public static <T> Result<T> error(String message){
        return new Result<>(500,message,null);
    }
}
```

---

# 7️⃣ 用户模块

# 7.1 用户功能

支持：

* 登录
* 注册
* 获取用户信息
* 用户分页
* 用户详情
* 修改用户
* 删除用户

---

# 7.2 用户接口

| 接口                | 请求方式   | 说明       |
| ----------------- | ------ | -------- |
| /user/login       | POST   | 用户登录     |
| /user/register    | POST   | 用户注册     |
| /user/info        | GET    | 获取当前用户信息 |
| /user/list        | GET    | 用户列表     |
| /user/page        | GET    | 用户分页     |
| /user/detail/{id} | GET    | 用户详情     |
| /user/update      | PUT    | 修改用户     |
| /user/delete/{id} | DELETE | 删除用户     |

---

# 7.3 LoginDTO

```java
@Data
public class LoginDTO {

    private String username;

    private String password;
}
```

---

# 7.4 UserRegisterDTO

```java
@Data
public class UserRegisterDTO {

    private String username;

    private String password;

    private String nickname;
}
```

---

# 7.5 UserVO

```java
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String role;
}
```

---

# 8️⃣ 项目模块

# 8.1 项目功能

支持：

* 项目新增
* 项目修改
* 项目删除
* 项目分页
* 项目详情
* 项目成员管理

---

# 8.2 项目接口

| 接口                               | 请求方式   | 说明     |
| -------------------------------- | ------ | ------ |
| /project/add                     | POST   | 新增项目   |
| /project/list                    | GET    | 项目列表   |
| /project/page                    | GET    | 项目分页   |
| /project/detail/{id}             | GET    | 项目详情   |
| /project/update                  | PUT    | 修改项目   |
| /project/delete/{id}             | DELETE | 删除项目   |
| /project/member/add              | POST   | 添加项目成员 |
| /project/member/list/{projectId} | GET    | 项目成员列表 |
| /project/member/delete/{id}      | DELETE | 删除项目成员 |

---

# 8.3 ProjectDTO

```java
@Data
public class ProjectDTO {

    private Long id;

    private String name;

    private String description;

    private Integer status;
}
```

---

# 8.4 ProjectVO

```java
@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String description;

    private Integer status;

    private String creatorName;

    private Integer taskCount;
}
```

---

# 9️⃣ 任务模块

# 9.1 任务功能

支持：

* 新增任务
* 修改任务
* 删除任务
* 任务详情
* 任务分页
* 任务状态修改
* 指派执行人
* 条件查询
* 任务统计

---

# 9.2 任务接口

| 接口                | 请求方式   | 说明     |
| ----------------- | ------ | ------ |
| /task/add         | POST   | 新增任务   |
| /task/list        | GET    | 任务列表   |
| /task/page        | GET    | 任务分页   |
| /task/detail/{id} | GET    | 任务详情   |
| /task/update      | PUT    | 修改任务   |
| /task/delete/{id} | DELETE | 删除任务   |
| /task/status      | PUT    | 修改任务状态 |
| /task/assign      | PUT    | 指派任务   |
| /task/statistics  | GET    | 任务统计   |

---

# 9.3 TaskDTO

```java
@Data
public class TaskDTO {

    private Long id;

    private Long projectId;

    private String title;

    private String content;

    private Long executorId;

    private Integer priority;

    private Integer status;
}
```

---

# 9.4 TaskVO

```java
@Data
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    private Integer status;

    private Integer priority;

    private String projectName;

    private String creatorName;

    private String executorName;
}
```

---

# 🔟 评论模块

# 10.1 评论功能

支持：

* 新增评论
* 评论列表
* 删除评论
* 回复评论

---

# 10.2 评论接口

| 接口                     | 请求方式   | 说明   |
| ---------------------- | ------ | ---- |
| /comment/add           | POST   | 新增评论 |
| /comment/list/{taskId} | GET    | 评论列表 |
| /comment/delete/{id}   | DELETE | 删除评论 |

---

# 10.3 CommentDTO

```java
@Data
public class CommentDTO {

    private Long taskId;

    private String content;

    private Long parentId;
}
```

---

# 10.4 CommentVO

```java
@Data
public class CommentVO {

    private Long id;

    private String content;

    private String nickname;

    private String avatar;

    private String createTime;
}
```

---

# 1️⃣1️⃣ 文件模块

# 11.1 文件功能

支持：

* 文件上传
* 文件列表
* 文件下载
* 文件删除

---

# 11.2 文件接口

| 接口                  | 请求方式   | 说明   |
| ------------------- | ------ | ---- |
| /file/upload        | POST   | 文件上传 |
| /file/list          | GET    | 文件列表 |
| /file/delete/{id}   | DELETE | 删除文件 |
| /file/download/{id} | GET    | 下载文件 |

---

# 11.3 文件上传规范

Content-Type：

```text
multipart/form-data
```

上传参数：

| 参数        | 类型            |
| --------- | ------------- |
| file      | MultipartFile |
| taskId    | Long          |
| projectId | Long          |

---

# 1️⃣2️⃣ 实时消息通知（WebSocket）

# 12.1 功能描述

支持服务端向特定用户推送实时通知（如：任务指派、评论回复）。

* **连接地址**: `ws://localhost:8080/ws/notification?userId={userId}`
* **消息结构**: `NotificationMessage { type, content, businessId, timestamp }`

---

# 12.2 核心实现

* **NotificationWebSocketHandler**: 维护 `ConcurrentHashMap<Long, WebSocketSession>` 存储在线用户会话。
* **sendMessage(userId, message)**: 静态方法，用于在 Service 层触发消息推送。

---

# 1️⃣3️⃣ 分页统一结构

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": []
  }
}
```

---

# 1️⃣4️⃣ 状态定义

# 项目状态

| 值 | 含义  |
| - | --- |
| 0 | 未开始 |
| 1 | 进行中 |
| 2 | 已完成 |

---

# 任务状态

| 值 | 含义  |
| - | --- |
| 0 | 待开始 |
| 1 | 进行中 |
| 2 | 已完成 |

---

# 任务优先级

| 值 | 含义 |
| - | -- |
| 0 | 普通 |
| 1 | 紧急 |

---

# 1️⃣5️⃣ 推荐 Entity

推荐创建：

```text
User
Project
ProjectMember
Task
Comment
FileInfo
Notification
OperationLog
Role
Permission
UserRole
RolePermission
```

---

# 1️⃣6️⃣ 推荐 DTO

推荐创建：

```text
LoginDTO
UserRegisterDTO
ProjectDTO
TaskDTO
CommentDTO
```

---

# 1️⃣7️⃣ 推荐 VO

推荐创建：

```text
UserVO
ProjectVO
TaskVO
CommentVO
```

---

# 1️⃣8️⃣ 推荐开发顺序

# 第一阶段

建议优先开发：

1. 用户模块
2. JWT
3. 项目模块
4. 任务模块
5. 评论模块
6. 文件模块

---

# 第二阶段

建议继续开发：

* Redis 缓存
* ECharts 数据统计
* WebSocket 消息通知
* 操作日志
* RBAC 权限控制

---

# 1️⃣9️⃣ 推荐配置

# application.yml

```yml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/collab_flow
    username: root
    password: 123456

  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

# 2️⃣0️⃣ 推荐依赖

# pom.xml

```xml
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.5</version>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt</artifactId>
        <version>0.9.1</version>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>

    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>

    <!-- WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- AOP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Hutool -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.26</version>
    </dependency>

    <!-- Fastjson2 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.52</version>
    </dependency>

</dependencies>
```

---

# 2️⃣1️⃣ 项目亮点

项目亮点：

* JWT + Redis 登录鉴权
* RBAC 权限控制系统
* 企业级模块化开发
* Spring Boot + Vue3 前后端分离
* MyBatis Plus 快速开发
* 文件上传系统
* 评论系统
* 分页查询
* 条件筛选
* 项目成员协作
* 任务统计
* 实时消息通知（WebSocket）
* 操作日志记录（AOP）
* 企业级接口设计

---

# 2️⃣2️⃣ 当前项目完成度

| 模块   | 完成度 |
| ---- | --- |
| 登录鉴权 | 100% |
| 用户模块 | 100% |
| 项目模块 | 100% |
| 任务模块 | 100% |
| 评论模块 | 100% |
| 文件模块 | 100% |
| 权限模块 | 100% |
| 消息通知 | 100% |
| 操作日志 | 100% |

整体项目完成度：

```text
约 100%
```

---

# 2️⃣3️⃣ 后续推荐优化

后续可以继续优化：

* Redis Token续期
* RBAC权限系统
* 消息通知
* 邮件通知
* WebSocket实时协作
* MinIO对象存储
* Docker部署
* Linux部署
* Nginx反向代理
* 邮件通知

---

# 2️⃣4️⃣ 后端启动方式

```bash
mvn clean install
mvn spring-boot:run
```

---

# 2️⃣5️⃣ Knife4j 文档地址

启动项目后访问：

```text
http://localhost:8080/doc.html
```

---

# 2️⃣6️⃣ 总结

Collab Flow 已具备：

* 企业级任务协作系统基础架构
* 前后端分离开发模式
* JWT + Redis 登录鉴权
* RBAC 权限控制
* CRUD完整体系
* 文件上传功能
* 评论系统
* 分页与条件筛选
* 项目成员协作
* 实时消息通知（WebSocket）
* 操作日志记录（AOP）

适合作为：

* Java Web课程设计
* Spring Boot实战项目
* 毕业设计项目
* 简历项目
