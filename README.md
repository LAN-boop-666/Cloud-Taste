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

```powershell
cd D:\workspace\cloud-taste
.\scripts\start-backend.ps1
```

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
| 完整链路 | 登录后台并打开分类管理 | 可以查询、新增、修改、启停和删除分类 |

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

已具备：

- 员工登录、JWT 身份验证和员工管理基础功能。
- 分类管理的分页、新增、修改、启用、禁用、删除和按类型查询。
- 分类删除前检查是否关联菜品或套餐。
- 通用文件上传接口及阿里云 OSS 配置入口。
- 新增菜品，同时保存菜品对应的口味数据。
- 管理后台、Nginx、MySQL 和后端的本地运行环境。

当前只完成了菜品新增，菜品分页、修改、删除和启停等功能仍应继续跟随课程实现；套餐管理尚未完整实现。

## 六、本地配置和提交安全

本机数据库配置位于：

```text
backend\sky-take-out\sky-server\src\main\resources\application-local.yml
```

这个文件已被 `.gitignore` 忽略。可以在本机填写数据库密码，但不要强制添加或上传。项目也支持 `CLOUD_TASTE_DB_PASSWORD` 等环境变量。

OSS 的 endpoint、AccessKey 和 bucket 也只写入 `application-local.yml`，或使用下面的环境变量：

```text
CLOUD_TASTE_OSS_ENDPOINT
CLOUD_TASTE_OSS_ACCESS_KEY_ID
CLOUD_TASTE_OSS_ACCESS_KEY_SECRET
CLOUD_TASTE_OSS_BUCKET_NAME
```

提交前至少执行：

```powershell
cd D:\workspace\cloud-taste
git status
```

不要提交 `application-local.yml`、`node_modules`、`target`、`.tools` 或 `.maven-repository`。

日常开发统一使用 `dev` 分支：

```powershell
cd D:\workspace\cloud-taste
git switch dev
git pull --ff-only origin dev
git add -A
git status
git commit -m "说明本次完成的功能"
git push origin dev
```

提交前必须先看一次 `git status`，确认列表里没有本机配置、构建产物或 IDE 文件。需要合并到受保护的 `master` 时，在 GitHub 创建从 `dev` 到 `master` 的 Pull Request，不直接推送 `master`。

## 七、详细文档入口

- [完整环境运行指南](docs/环境运行指南.md)：IDEA、Nginx、前端源码和 PowerShell 的详细操作。
- [环境安装记录](docs/环境安装记录.md)：已安装版本、路径、占用和后续清理依据。
- [数据库设计](docs/database-design.md)：数据库表和字段说明。
- [管理端接口文档](docs/api/admin-api.html)：课程管理端 API 文档。
- [学习进度](docs/learning/learning-progress.md)：课程学习记录。
- [项目路线](docs/learning/project-roadmap.md)：课程阶段和后期扩展方向。
- [Git 仓库恢复记录](docs/Git仓库恢复记录.md)：本次分支、历史和配置污染问题的原因及后续规则。

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
