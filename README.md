# 云味餐厅 CloudTaste

基于黑马程序员《苍穹外卖》课程学习的餐饮管理项目。前期保持课程的模块、包名、接口和运行方式，完成课程后再进行原创扩展。

> **从这里开始：** IDEA 只打开 `D:\workspace\cloud-taste\backend\sky-take-out`，运行 `SkyApplication.java`；管理后台使用 Nginx，浏览器访问 `http://localhost/`。

## 一、项目怎么打开

| 要做什么 | 使用工具 | 打开位置 |
| --- | --- | --- |
| 编写、运行 Java 后端 | IDEA | `D:\workspace\cloud-taste\backend\sky-take-out` |
| 直接运行课程管理后台 | 文件资源管理器 | `D:\workspace\cloud-taste\admin-dist\nginx-1.20.2` |
| 修改管理后台源码 | VS Code | `D:\workspace\cloud-taste\admin-web\project-sky-admin-vue-ts` |
| 查看和管理数据库 | DataGrip | 数据库 `cloud_taste`，MySQL 端口 `3306` |
| 查看整个仓库 | IDEA、VS Code 或文件资源管理器 | `D:\workspace\cloud-taste` |

IDEA 中的后端启动类：

```text
sky-server\src\main\java\com\sky\SkyApplication.java
```

不要在 IDEA 中只打开 `sky-server`，也不要把整个 `cloud-taste` 当作 Maven 后端工程导入。课程后端是由 `sky-common`、`sky-pojo`、`sky-server` 组成的多模块 Maven 项目。

## 二、每次上课前怎么启动

完整运行顺序：

```text
MySQL -> Spring Boot 后端 -> Nginx 管理后台
```

### 方式 A：按照黑马课程操作

1. 确认 Windows 服务中的 `MySQL80` 已启动。
2. IDEA 打开 `D:\workspace\cloud-taste\backend\sky-take-out`。
3. 运行 `SkyApplication.java`，看到后端启动成功并监听 `8080`。
4. 打开 `D:\workspace\cloud-taste\admin-dist\nginx-1.20.2`。
5. 双击 `nginx.exe`，然后访问 `http://localhost/`。

页面可以打开但一直无法登录，通常表示只启动了 Nginx，后端或 MySQL 没有启动。

### 方式 B：使用项目脚本

在两个 PowerShell 窗口中分别执行：

启动后端：

```powershell
cd D:\workspace\cloud-taste
.\scripts\start-backend.ps1
```

启动 Nginx：

```powershell
cd D:\workspace\cloud-taste
.\scripts\start-nginx.ps1
```

停止 Nginx：

```powershell
cd D:\workspace\cloud-taste
.\scripts\stop-nginx.ps1
```

## 三、启动后怎么验收

| 检查内容 | 地址或方法 | 正常现象 |
| --- | --- | --- |
| 管理后台 | `http://localhost/` | 出现登录页面 |
| 后端接口文档 | `http://127.0.0.1:8080/doc.html` | 出现 Swagger/Knife4j 文档 |
| 后端端口 | `http://127.0.0.1:8080` | 后端进程监听 `8080` |
| 数据库 | DataGrip 连接 `cloud_taste` | 可以看到课程基础表 |
| 完整链路 | 登录后台并打开分类、菜品和套餐管理 | 页面可以正常查询，相关新增、修改、启停和删除接口可用 |

Apifox 调试真实后端时使用：

```text
直连 Spring Boot：http://127.0.0.1:8080/admin/...
经过 Nginx：http://127.0.0.1/api/...
```

不要使用带有 `4523/m1/...` 的 Mock 地址判断后端是否成功。HTTP 状态为 `200` 也不等于业务成功，还要检查响应体中的 `code` 是否为 `1`。

## 四、目录是做什么的

```text
cloud-taste
├─ backend
│  └─ sky-take-out                 Java 后端，IDEA 打开这里
├─ admin-dist
│  └─ nginx-1.20.2                 课程打包版前端和 Nginx
├─ admin-web
│  └─ project-sky-admin-vue-ts     Vue 2 管理端源码
├─ database
│  └─ cloud_taste.sql              数据库初始化脚本
├─ docs                            详细资料、接口文档和学习记录
├─ scripts                         启动、构建和检查脚本
└─ README.md                       项目总入口，也就是当前文件
```

后端模块：

```text
sky-common    公共常量、异常、工具和统一返回结果
sky-pojo      Entity、DTO、VO 等数据对象
sky-server    Controller、Service、Mapper、配置和启动类
```

`admin-dist` 是可以直接运行的课程成品前端；`admin-web` 是需要 Node 环境的前端源码。只是跟课验证后端时，优先使用 `admin-dist`。

## 五、当前学习进度

已完成课程前四天的核心管理端功能：

- 员工登录、JWT 身份验证和员工管理基础功能。
- 分类管理的分页、新增、修改、启用、禁用、删除和按类型查询。
- 分类删除前检查是否关联菜品或套餐。
- 菜品管理的新增、分页、批量删除、详情回显和修改。
- 菜品和口味数据的关联保存、批量删除及重新保存。
- 套餐管理的新增、分页、批量删除、详情回显、修改和起售停售。
- 套餐与菜品关系的批量保存，以及套餐起售前的菜品状态检查。
- 不存在的套餐会返回“套餐不存在”的业务错误，不再触发空指针异常。
- 管理后台、Nginx、MySQL 和后端的本地运行环境。

day04 主要是使用 day03 已经学过的 Controller、Service、Mapper、事务、动态 SQL 和关联表处理方式，独立完成套餐管理。它更偏向综合练习，不是引入一套新的框架知识。

day04 当前验证结果：

```text
Maven 全模块：BUILD SUCCESS
套餐业务测试：9 个通过，0 失败，0 错误
Spring Boot：启动成功
MySQL / MyBatis：连接和查询正常
```

下一步按照课程进入 day05。在开始前先验收管理后台中的菜品和套餐页面，确认列表查询、详情回显和业务提示符合预期。

## 六、本地配置和提交安全

本机数据库配置位于：

```text
backend\sky-take-out\sky-server\src\main\resources\application-local.yml
```

这个文件已被 `.gitignore` 忽略。可以在本机填写数据库密码，但不要强制添加或上传。项目也支持 `CLOUD_TASTE_DB_PASSWORD` 等环境变量。

提交前至少执行：

```powershell
cd D:\workspace\cloud-taste
git status
```

不要提交 `application-local.yml`、`node_modules`、`target`、`.tools` 或 `.maven-repository`。

## 七、详细文档入口

- [完整环境运行指南](docs/环境运行指南.md)：IDEA、Nginx、前端源码和 PowerShell 的详细操作。
- [环境安装记录](docs/环境安装记录.md)：已安装版本、路径、占用和后续清理依据。
- [数据库设计](docs/database-design.md)：数据库表和字段说明。
- [管理端接口文档](docs/api/admin-api.html)：课程管理端 API 文档。
- [学习进度](docs/learning/learning-progress.md)：课程学习记录。
- [Day09 订单管理学习文档](docs/learning/day09-order-management.md)：订单生命周期、接口和配送范围校验。
- [项目路线](docs/learning/project-roadmap.md)：课程阶段和后期扩展方向。

## 八、常见问题

### 前端页面能打开，但登录一直加载

Nginx 只负责页面和接口转发。还需要启动 `SkyApplication.java`，并确保 MySQL 正常运行、数据库密码正确。

### IDEA 的 GitHub 拉取请求页面提示受限

这是 IDEA GitHub 插件或网络访问 GitHub API 的问题，不代表本地 Git 仓库损坏。普通提交、拉取和推送请使用 IDEA 的 Git 菜单或项目根目录中的 Git 命令。

### 只想看前端

双击 `admin-dist\nginx-1.20.2\nginx.exe`，访问 `http://localhost/`。页面可以浏览，但登录和数据操作仍然需要后端。

### 想运行前端源码

```powershell
cd D:\workspace\cloud-taste
.\scripts\start-admin-dev.ps1
```

默认访问 `http://127.0.0.1:8889/`。该脚本使用项目内 Node 12，不会替换系统 Node。
