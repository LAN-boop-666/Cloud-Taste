# Redis 使用说明

## 一键启动和停止

项目根目录中有两个文件：

```text
启动Redis.cmd
停止Redis.cmd
```

双击 `启动Redis.cmd`，Redis 会在后台运行，命令窗口随后自动关闭。

双击 `停止Redis.cmd`，Redis 会保存数据并安全停止，命令窗口随后自动关闭。

重复点击启动文件不会产生多个 Redis 进程。

## 连接参数

```text
Host: 127.0.0.1
Port: 6379
Username: 留空
Password: 留空
Database: 2
```

## 命令行用法

先进入 Redis 目录：

```powershell
cd D:\workspace\cloud-taste\.tools\redis-3.2.100
```

使用配置文件启动：

```powershell
.\redis-server.exe .\redis-cloud-taste.conf
```

连接 Redis：

```powershell
.\redis-cli.exe -h 127.0.0.1 -p 6379 -n 2
```

常用命令：

```powershell
# 检查 Redis 是否启动，正常返回 PONG
.\redis-cli.exe -h 127.0.0.1 -p 6379 ping

# 连接项目使用的 DB2
.\redis-cli.exe -h 127.0.0.1 -p 6379 -n 2

# 查看 DB2 中有多少个 Key
.\redis-cli.exe -h 127.0.0.1 -p 6379 -n 2 dbsize

# 扫描 DB2 中的 Key
.\redis-cli.exe -h 127.0.0.1 -p 6379 -n 2 scan 0

# 安全停止 Redis
.\redis-cli.exe -h 127.0.0.1 -p 6379 shutdown
```

常用连接参数：

| 参数 | 含义 | 示例 |
| --- | --- | --- |
| `-h` | 主机地址 | `-h 127.0.0.1` |
| `-p` | 端口 | `-p 6379` |
| `-n` | 逻辑数据库编号 | `-n 2` |
| `-a` | 密码 | 当前未设置，不使用 |
| `--raw` | 直接显示字符串 | 查看中文时可使用 |

## 常用 Redis 命令

下面的命令默认已经进入 Redis 交互窗口：

```powershell
.\redis-cli.exe -h 127.0.0.1 -p 6379 -n 2
```

### 服务器和数据库

```redis
PING                         # 测试连接，返回 PONG
INFO                         # 查看 Redis 服务信息
INFO memory                  # 查看内存信息
CLIENT LIST                  # 查看客户端连接
DBSIZE                       # 查看当前数据库的 Key 数量
SELECT 2                     # 切换到项目使用的 DB2
FLUSHDB                      # 清空当前数据库，危险
FLUSHALL                     # 清空所有数据库，危险
```

### Key 操作

```redis
SET user:1 LAN               # 写入字符串
GET user:1                   # 读取字符串
EXISTS user:1                # 判断 Key 是否存在，返回 1 或 0
TYPE user:1                  # 查看 Key 的数据类型
TTL user:1                   # 查看剩余过期时间，-1 表示永不过期，-2 表示不存在
EXPIRE user:1 3600           # 设置 3600 秒后过期
PERSIST user:1               # 移除过期时间
RENAME user:1 user:2         # 修改 Key 名称
DEL user:2                   # 删除 Key
SCAN 0                       # 分批扫描 Key，适合数据较多时
```

`KEYS *` 可以快速查看所有 Key，但数据量大时会阻塞 Redis，学习环境以 `SCAN 0` 为主：

```redis
KEYS *                      # 仅适合当前空库或少量数据
SCAN 0 MATCH dish:* COUNT 100
```

### String 字符串

```redis
SET name LAN                 # 设置值
GET name                     # 获取值
MSET name LAN age 18         # 一次设置多个值
MGET name age                # 一次获取多个值
SET counter 1                # 设置数字字符串
INCR counter                 # 加 1，返回 2
INCRBY counter 10            # 增加指定数值
DECR counter                 # 减 1
APPEND name " Taste"        # 追加字符串
```

### Hash 哈希

```redis
HMSET user:1 name LAN age 18  # Redis 3.2 设置多个字段
HGET user:1 name              # 获取一个字段
HMGET user:1 name age         # 获取多个字段
HGETALL user:1                # 获取全部字段和值
HEXISTS user:1 name           # 判断字段是否存在
HINCRBY user:1 age 1          # 数字字段加 1
HDEL user:1 age               # 删除字段
```

### List 列表

```redis
LPUSH queue order:1            # 从左侧加入元素
RPUSH queue order:2            # 从右侧加入元素
LRANGE queue 0 -1             # 查看全部元素
LLEN queue                     # 查看列表长度
LPOP queue                     # 从左侧取出一个元素
RPOP queue                     # 从右侧取出一个元素
```

### Set 集合

```redis
SADD tags java redis spring    # 添加元素，重复元素不会重复保存
SMEMBERS tags                  # 查看全部元素
SISMEMBER tags redis           # 判断元素是否存在
SCARD tags                     # 查看元素数量
SREM tags java                 # 删除元素
```

### Sorted Set 有序集合

```redis
ZADD ranking 100 LAN           # 添加成员和分数
ZADD ranking 90 Tom
ZRANGE ranking 0 -1 WITHSCORES # 按分数从低到高查看
ZREVRANGE ranking 0 -1 WITHSCORES # 按分数从高到低查看
ZSCORE ranking LAN             # 查看成员分数
ZREM ranking Tom               # 删除成员
```

### 清理学习数据

```redis
DEL user:1 user:2 counter      # 删除指定 Key
DEL queue tags ranking         # 删除本节示例数据
FLUSHDB                        # 清空当前 DB，确认无重要数据后再用
```

不要在项目数据库中随意执行 `FLUSHALL`，它会清空 DB0 到 DB15 的全部数据。

## 文件位置

```text
Redis 程序：D:\workspace\cloud-taste\.tools\redis-3.2.100
配置文件：D:\workspace\cloud-taste\.tools\redis-3.2.100\redis-cloud-taste.conf
数据目录：D:\workspace\cloud-taste\.tools\redis-3.2.100\data
日志文件：D:\workspace\cloud-taste\.tools\redis-3.2.100\logs\redis.log
```

`.tools` 已被 Git 忽略，Redis 程序、数据和日志不会上传到仓库。
