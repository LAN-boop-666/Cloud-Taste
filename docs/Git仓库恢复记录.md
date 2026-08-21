# Git 仓库恢复记录

## 发生了什么

2026-08-22 在处理受保护的 `master` 分支时，错误地创建并提交了不完整的历史，随后又从错误提交创建 `dev`。这个 `dev` 只保留了极少量源配置，却提交了 Maven `target`、编译后的 class/JAR、IDEA 配置和本机 `application-local.yml`。

两个名为“新增菜品业务代码开发”的本地提交实际上各自只修改了一个 YAML 文件，不能作为源码恢复依据。远端 `master` 没有被覆盖，仍保存了员工、分类、公共字段自动填充和项目文档，因此本次以 `origin/master` 为恢复基线。

菜品新增、口味批量保存、文件上传和 OSS 配置代码没有出现在正确提交中，但编译产物仍保留了类签名、方法和 SQL。恢复时依据这些编译信息重建源码，并通过 Maven 完整编译验证。

## 本次修复

- 从 `origin/master` 创建干净开发历史。
- 恢复文件上传、OSS 配置、菜品新增及口味批量保存代码。
- 删除误提交的 Node 诊断报告。
- 根目录 `.gitignore` 统一忽略本机配置、IDE 文件、依赖、缓存、日志和构建产物。
- `application-dev.yml` 只保留环境变量占位，不保存真实密码或 AccessKey。
- `application-local.yml` 仅供本机使用，不加入 Git。

## 正确分支关系

```text
origin/master
    |
    +-- dev（正常开发和推送）
            |
            +-- Pull Request -> master
```

`dev` 必须从 `origin/master` 的正常历史创建，不能使用 `git checkout --orphan`。孤儿分支只适合确实需要完全舍弃原历史的特殊场景。

## 日常提交

```powershell
cd D:\workspace\cloud-taste
git switch dev
git pull --ff-only origin dev
git add -A
git status
git commit -m "说明本次完成的功能"
git push origin dev
```

在执行 `git commit` 前检查 `git status`。若出现以下内容，应先停止提交并检查忽略规则：

```text
application-local.yml
target
node_modules
.idea
.tools
.maven-repository
*.class
*.jar
*.log
```

## 配置安全

已上传过的密钥不能仅靠删除文件或重写分支保证失效。涉及真实数据库密码或云服务 AccessKey 时，应在对应服务中更换凭据。新凭据只保存在本机 `application-local.yml` 或环境变量中。
