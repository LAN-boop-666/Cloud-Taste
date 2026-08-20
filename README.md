# 云味餐厅 CloudTaste

这是 LAN 的餐饮点餐系统学习工程，以 B 站黑马程序员《苍穹外卖》课程为学习主线，并在关键节点补充现代开发习惯。

## 目录

- `backend/sky-take-out`：Day01 后端初始工程
- `admin-web/project-sky-admin-vue-ts`：管理端 Vue 源码，暂按课程版本保留
- `admin-dist/nginx-1.20.2`：课程提供的可运行管理端静态文件和 Nginx
- `database/cloud_taste.sql`：本地学习数据库脚本
- `docs/api`：接口文档
- `docs/prototype`：产品原型压缩包
- `docs/learning`：项目实战任务
- `docs/环境运行指南.md`：IDEA、VS Code、PowerShell 和各服务运行方式
- `docs/环境安装记录.md`：本项目安装路径、版本和清理记录

## 学习约定

第一遍以理解需求、接口、数据库、分层结构、事务、缓存和订单流程为主。`com.sky`、`sky-common`、`sky-pojo`、`sky-server`、表名和接口路径暂时保留，便于逐节对照视频。

课程最终工程只留在课程资料目录中，不复制到本项目作为实现答案。遇到“导入代码”的章节，先阅读需求和接口，再逐段完成并验证。

## 本地配置

后端默认读取 `application-dev.yml` 和本机专用的 `application-local.yml`。
首次使用时，将
`backend/sky-take-out/sky-server/src/main/resources/application-local.yml.example`
复制为 `application-local.yml`，再填写自己的数据库密码。`application-local.yml`
已被 `.gitignore` 忽略，不会提交到 Git。

数据库配置也支持以下环境变量，环境变量优先级更高：

- `CLOUD_TASTE_DB_HOST`，默认 `localhost`
- `CLOUD_TASTE_DB_PORT`，默认 `3306`
- `CLOUD_TASTE_DB_NAME`，默认 `cloud_taste`
- `CLOUD_TASTE_DB_USERNAME`，默认 `root`
- `CLOUD_TASTE_DB_PASSWORD`，无默认值
- `CLOUD_TASTE_JWT_SECRET`，默认仅用于本机学习的占位值

不要把真实密码、JWT 密钥、微信配置或 OSS 配置写入 Git、接口文档或聊天记录。

## 构建说明

IDEA 使用 Bundled Maven 3.9.6，Java 使用 17。命令行构建优先使用后端目录中的 Maven Wrapper（Maven 3.9.6），不依赖全局 `mvn`。

管理端源码是 Vue 2 / Vue CLI 3 工程。第一阶段优先使用 `admin-dist` 的成品，源码编译放到前端章节单独处理。

## 本地运行顺序

1. 在 PowerShell 中设置 `CLOUD_TASTE_DB_PASSWORD`，或运行 `scripts/init-database.ps1` 按提示输入密码。
2. 运行 `scripts/build-backend.ps1` 构建后端。
3. 运行 `scripts/start-backend.ps1` 启动后端，默认端口 `8080`。
4. 另开终端运行 `scripts/start-nginx.ps1`，访问 `http://127.0.0.1/`。
5. 后端和数据库都启动后运行 `scripts/test-day01.ps1`。

停止前端代理：`scripts/stop-nginx.ps1`。

首次数据库导入前请确认 `cloud_taste` 尚不存在。初始化脚本发现数据库已存在时会停止，不会覆盖表数据。
