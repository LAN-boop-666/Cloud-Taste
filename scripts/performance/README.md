# Cloud-Taste 性能测试

## 目标

使用隔离数据库 `cloud_taste_loadtest` 测试用户端商品、购物车和提交订单接口，不触碰原来的 `cloud_taste` 业务库。

## 当前环境

- 测试后端：`http://127.0.0.1:8081`
- 正常开发后端：`http://127.0.0.1:8080`
- MySQL 测试库：`cloud_taste_loadtest`
- Redis：`127.0.0.1:6379`
- JMeter：`D:\workspace\cloud-taste\.tools\apache-jmeter`

## JMeter 测试计划

文件：`order-load-test.jmx`

每个虚拟用户从 `.tools\loadtest-users.csv` 读取自己的 `userId`、`addressBookId` 和 JWT。测试流程为：

```text
添加测试菜品到购物车 -> 提交订单
```

测试计划不会调用真实微信支付，也不会把密码写入 JMeter 文件。

## 准备隔离环境

下面的命令只会重建 `cloud_taste_loadtest`，不会修改 `cloud_taste`：

```powershell
& 'scripts\performance\prepare-loadtest.ps1' -ConfirmRecreate
```

脚本会导入项目表结构、生成测试数据，并在 `.tools\loadtest-users.csv` 生成两小时有效的本地测试 Token。密码和密钥只从本地配置读取，不会打印或写入仓库。

## 命令行运行

在项目根目录执行：

```powershell
& '.tools\apache-jmeter\bin\jmeter.bat' -n -t 'scripts\performance\order-load-test.jmx' -Jthreads=5 -JrampUp=5 -Jloops=1 -JusersFile='.tools\loadtest-users.csv' -l '.tools\loadtest-results\order-5.jtl' -e -o '.tools\loadtest-results\report-5'
```

也可以使用封装脚本：

```powershell
& 'scripts\performance\run-jmeter.ps1' -Scenario order -Threads 5 -RampUp 5 -Loops 1
& 'scripts\performance\run-jmeter.ps1' -Scenario query -Threads 20 -RampUp 5 -Loops 5
```

逐级测试：

```text
threads=5
threads=10
threads=20
threads=50
```

上一档完成并确认错误率正常后再进入下一档。不要在 GUI 监听器中运行正式压力测试。

## Apifox 用法

Apifox 适合先验证单接口和小规模场景。请求地址使用：

```text
http://127.0.0.1:8081
```

Apifox 可以直接导入测试实例的 Swagger 地址：

```text
http://127.0.0.1:8081/v2/api-docs?group=user接口
```

请求头：

```text
authentication: 测试用户 JWT
Content-Type: application/json
```

提交订单请求体：

```json
{
  "addressBookId": 10001,
  "payMethod": 1,
  "remark": "LOADTEST",
  "deliveryStatus": 1,
  "tablewareNumber": 1,
  "tablewareStatus": 1,
  "packAmount": 0,
  "amount": 0
}
```

## 清理

压测结果、测试用户 CSV 和临时日志都在 `.tools` 下，并被 Git 忽略。数据库清理脚本是：

```text
scripts\performance\loadtest-cleanup.sql
```

执行前确认当前连接的数据库是 `cloud_taste_loadtest`，不要对 `cloud_taste` 执行。

也可以在测试后端已经停止的情况下执行：

```powershell
& 'scripts\performance\cleanup-loadtest.ps1' -ConfirmDrop
```

## 结果记录

每轮记录：

- 样本数、成功数、失败数和错误率
- 平均响应时间、P95、P99、最大响应时间
- 吞吐量
- MySQL CPU、连接数、慢查询
- Redis 状态

`select *`、全表扫描和 `INNER JOIN` 是不同概念。优化前后需要分别记录 `EXPLAIN`，不能仅凭 SQL 写法判断性能。
