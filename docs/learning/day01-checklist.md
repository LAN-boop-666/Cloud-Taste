# Day01 开工清单

## 环境

- [x] Java 17 可用
- [x] IDEA 内置 Maven 3.9.6 可用
- [x] MySQL 8.0 服务可用
- [x] 后端初始工程已整理
- [x] 管理端源码和 Nginx 成品已整理
- [x] 数据库、接口文档和产品原型已整理
- [x] Maven 构建通过
- [x] `cloud_taste` 数据库导入完成（11 张表）
- [ ] 后端启动成功
- [ ] Knife4j 接口文档可访问
- [ ] 管理端登录请求联调通过

数据库已完成首次初始化。下一步启动后端，在本机终端中隐藏输入 MySQL 密码，然后验证接口文档和管理端登录链路。

## Day01 必须理解

- `sky-common`、`sky-pojo`、`sky-server` 各自负责什么
- 登录请求如何从 Nginx 到 Controller、Service、Mapper 和 MySQL
- DTO、Entity、VO 为什么不能混为一个对象
- JWT 在登录和后续请求中分别做什么
- 为什么数据库密码和签名密钥不能提交到 Git

## 验收接口

- `POST /admin/employee/login`
- `POST /admin/employee/logout`
- `/doc.html`
