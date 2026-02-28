# MaCache 从 0 到 1 开发计划

> 类比：MaCache 就像一个**快递柜**。客户端是寄件人/取件人，RESP 协议是快递单格式，Netty 是快递员，MaCache 是柜子本身，Command 是操作规则（存/取/查）。每个格子（key）可以放不同类型的包裹（String/List/Set/Hash/ZSet）。

---

## 总体版本路线图

```
v1.0  基础骨架 + String 类型          ← 已完成核心，需补测试
  │
v2.0  完整五种数据类型                 ← 已完成，需补测试
  │
v3.0  健壮性：粘包修复 + TTL + 类型检查
  │
v4.0  持久化：AOF 日志
  │
v5.0  高可用：主从复制
```

---

## v1.0 — 最小可用的缓存服务器

### 目标
搭建完整的网络 → 协议 → 命令 → 存储链路，支持 String 类型的基本读写。

### 核心概念类比

| 概念 | 类比 |
|------|------|
| Netty NIO | 一个能同时接待多个顾客的前台，不需要每人配一个服务员 |
| RESP 协议 | 快递单的标准格式，双方都按这个格式填写才能读懂 |
| Command 接口 | 操作手册中的一条规则，规定"收到这个指令，执行这个动作" |
| MaCache HashMap | 一排带编号的储物柜，key 是柜号，value 是里面的东西 |

### 功能列表
- [x] Spring Boot 启动容器
- [x] Netty NIO 服务器（端口 6379）
- [x] RESP 协议解码（MaCacheDecoder）
- [x] 命令分发（MaCacheHandler + Commands 注册表）
- [x] RESP 协议编码（5 种响应类型）
- [x] 通用命令：PING、INFO、COMMAND
- [x] String 命令：SET、GET、STRLEN、DEL、EXISTS、INCR、DECR、MSET、MGET

### 架构流程图

```
redis-cli
    │  发送: "*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"
    ▼
MaCacheDecoder          ← 把 TCP 字节流转成字符串
    │
    ▼
MaCacheHandler          ← 按 \r\n 分割，取 args[2] 作为命令名
    │  cmd = "SET"
    ▼
Commands.get("SET")     ← 从 LinkedHashMap 查找命令实现
    │
    ▼
SetCommand.exec()       ← 调用 cache.set(key, value)
    │
    ▼
MaCache.map             ← HashMap<String, CacheEntry<?>> 写入数据
    │
    ▼
Reply.string("OK")      ← 封装响应
    │
    ▼
MaCacheHandler          ← 编码为 "+OK\r\n" 写回客户端
    │
    ▼
redis-cli 显示: OK
```

### 测试流程（v1.0）

前提：启动 MaCache 服务（`mvn spring-boot:run`），确保端口 6379 可用。

```bash
# 连接服务器
redis-cli -p 6379

# 1. 连通性测试
PING
# 期望: PONG

# 2. 基本读写
SET name kimmking
# 期望: OK
GET name
# 期望: "kimmking"

# 3. 不存在的 key
GET notexist
# 期望: (nil)

# 4. 字符串长度
STRLEN name
# 期望: (integer) 8

# 5. 计数器
SET counter 10
INCR counter
# 期望: (integer) 11
DECR counter
# 期望: (integer) 10

# 6. 批量操作
MSET k1 v1 k2 v2 k3 v3
MGET k1 k2 k3
# 期望: 1) "v1"  2) "v2"  3) "v3"

# 7. 删除与存在性
EXISTS name
# 期望: (integer) 1
DEL name
EXISTS name
# 期望: (integer) 0

# 8. 服务器信息
INFO
COMMAND
```

---

## v2.0 — 完整五种数据类型

### 目标
在 v1.0 基础上，补全 List、Set、Hash、ZSet 四种数据类型的命令支持。

### 核心概念类比

| 数据类型 | 类比 |
|----------|------|
| List | 一条双向传送带，可以从两端放入或取出货物（LPUSH/RPUSH/LPOP/RPOP） |
| Set | 一个不允许重复的抽奖箱，可以随机抽取（SPOP） |
| Hash | 一张表格，每行是 field-value 对，适合存一个对象的多个属性 |
| ZSet | 带积分排行榜，每个成员有分数，自动按分数排序（ZRANK/ZRANGE） |

### 新增功能列表
- [x] List：LPUSH、RPUSH、LPOP、RPOP、LLEN、LINDEX、LRANGE
- [x] Set：SADD、SMEMBERS、SREM、SCARD、SPOP、SISMEMBER
- [x] Hash：HSET、HGET、HGETALL、HLEN、HDEL、HEXISTS、HMGET
- [x] ZSet：ZADD、ZCARD、ZSCORE、ZREM、ZRANK、ZCOUNT

### 数据存储结构图

```
MaCache.map (HashMap)
    │
    ├── "mylist"  → CacheEntry<LinkedList<String>>
    │                   ["a", "b", "c"]
    │
    ├── "myset"   → CacheEntry<LinkedHashSet<String>>
    │                   {"x", "y", "z"}
    │
    ├── "myhash"  → CacheEntry<LinkedHashMap<String,String>>
    │                   {name:"kim", age:"30"}
    │
    └── "myzset"  → CacheEntry<LinkedHashSet<ZsetEntry>>
                        [{val:"alice", score:100.0},
                         {val:"bob",   score:90.0}]
```

### 命令扩展流程图

```
新增一个命令（以 LRANGE 为例）:

1. 创建 LrangeCommand.java
       └── name()  → "LRANGE"
       └── exec()  → 解析 start/end 参数
                  → 调用 cache.lrange(key, start, end)
                  → 返回 Reply.array(result)

2. 在 Commands.initCommands() 注册
       └── register(new LrangeCommand())

3. MaCacheHandler 自动路由，无需修改
```

### 测试流程（v2.0）

```bash
redis-cli -p 6379

# === List 测试 ===
RPUSH mylist a b c
# 期望: (integer) 3
LRANGE mylist 0 -1
# 期望: 1) "a"  2) "b"  3) "c"
LPUSH mylist x
LINDEX mylist 0
# 期望: "x"
LPOP mylist
# 期望: "x"
LLEN mylist
# 期望: (integer) 3

# === Set 测试 ===
SADD myset apple banana cherry
SMEMBERS myset
# 期望: 3 个元素（顺序不定）
SISMEMBER myset apple
# 期望: (integer) 1
SREM myset banana
SCARD myset
# 期望: (integer) 2
SPOP myset 1
# 期望: 随机弹出一个元素

# === Hash 测试 ===
HSET user name kimmking age 30 city beijing
HGET user name
# 期望: "kimmking"
HGETALL user
# 期望: name, kimmking, age, 30, city, beijing
HEXISTS user email
# 期望: (integer) 0
HDEL user city
HLEN user
# 期望: (integer) 2
HMGET user name age
# 期望: 1) "kimmking"  2) "30"

# === ZSet 测试 ===
ZADD leaderboard 100 alice 90 bob 95 carol
ZCARD leaderboard
# 期望: (integer) 3
ZSCORE leaderboard alice
# 期望: "100"
ZRANK leaderboard bob
# 期望: (integer) 0  (bob 分数最低，排名第0)
ZCOUNT leaderboard 90 100
# 期望: (integer) 3
ZREM leaderboard bob
ZCARD leaderboard
# 期望: (integer) 2
```

---

## v3.0 — 健壮性增强

### 目标
修复已知缺陷，让服务器在真实场景下稳定运行。

### 问题背景类比
- **TCP 粘包**：就像快递员把两个包裹塞进一个袋子送来，收件人需要按快递单分开处理，而不是把整袋当一个包裹。
- **TTL 过期**：就像储物柜有租期，到期自动清空格子，不需要人工清理。
- **类型检查**：就像储物柜规定"A 区只放冷链货"，往 A 区放常温货时直接报错，而不是等取货时才发现问题。

### 功能列表

#### 3.1 修复 TCP 粘包/拆包
- 当前 `MaCacheDecoder` 直接读取所有字节，依赖客户端单次发完整命令
- 改造方案：实现基于 RESP 协议的有状态解码器
  - 读取第一个字节判断消息类型（`*` 表示数组）
  - 按 `\r\n` 分隔读取行，根据长度字段精确读取 bulk string
  - 使用 Netty `ReplayingDecoder` 或手动维护解析状态机

```
TCP 字节流（可能跨多个 packet）:
"*3\r\n$3\r\nSET\r\n" | "$3\r\nfoo\r\n$3\r\nbar\r\n"
        packet 1              packet 2

状态机:
INIT → READ_ARRAY_LEN → READ_BULK_LEN → READ_BULK_DATA → 完整命令
```

#### 3.2 TTL / EXPIRE 支持
- `CacheEntry` 增加 `expireAt: long` 字段（毫秒时间戳，-1 表示永不过期）
- 支持命令：`EXPIRE key seconds`、`TTL key`、`PERSIST key`、`PEXPIRE`、`PTTL`
- 惰性删除：每次 GET/访问时检查是否过期，过期则删除并返回 nil
- 定期删除：后台线程每 100ms 扫描一批 key，清理已过期的条目

```
TTL 流程:
SET foo bar
EXPIRE foo 60          → CacheEntry.expireAt = now + 60000ms

GET foo (50秒后)       → 检查 expireAt > now → 正常返回
GET foo (61秒后)       → 检查 expireAt < now → 删除 + 返回 nil
```

#### 3.3 类型安全检查
- 当前对错误类型操作会抛出 `ClassCastException`
- 在 `MaCache` 每个方法入口增加类型校验
- 类型不匹配时返回 Redis 标准错误：`WRONGTYPE Operation against a key holding the wrong kind of value`

### 测试流程（v3.0）

```bash
# === 粘包测试（用 nc 模拟分片发送）===
# 在两个终端分别执行，验证服务器能正确处理分片数据
printf "*3\r\n\$3\r\nSET\r\n" | nc -q1 localhost 6379 &
printf "\$5\r\nhello\r\n\$5\r\nworld\r\n" | nc localhost 6379

# === TTL 测试 ===
SET session_token abc123
EXPIRE session_token 5
TTL session_token
# 期望: (integer) 5
# 等待 6 秒
GET session_token
# 期望: (nil)
TTL session_token
# 期望: (integer) -2  (key 不存在)

# === 类型安全测试 ===
SET mystr hello
LPUSH mystr world
# 期望: (error) WRONGTYPE Operation against a key holding the wrong kind of value
```

---

## v4.0 — AOF 持久化

### 目标
服务器重启后数据不丢失，通过追加写日志（AOF）实现持久化。

### 核心概念类比
AOF（Append Only File）就像**银行流水账**：每笔操作都记录下来，即使银行系统崩溃，重新按流水账执行一遍，账户余额就能恢复原状。

### 功能列表

#### 4.1 AOF 写入
- 每次执行写命令（SET/DEL/LPUSH 等）后，将 RESP 格式的命令追加写入 `kkcache.aof` 文件
- 写入策略（可配置）：
  - `always`：每条命令同步刷盘（最安全，最慢）
  - `everysec`：每秒刷盘一次（默认，平衡）
  - `no`：由 OS 决定（最快，可能丢数据）

#### 4.2 AOF 加载（重放）
- 服务启动时，若 AOF 文件存在，逐行读取并重新执行命令，恢复内存状态

#### 4.3 AOF 重写（压缩）
- AOF 文件会随时间增大，重写时扫描当前内存状态，生成最小化的等效命令集
- 支持命令：`BGREWRITEAOF`

```
AOF 写入流程:
Command.exec() 成功
    │
    ▼
AOFWriter.append(respCommand)   ← 追加到文件
    │
    ▼
根据 fsync 策略决定何时刷盘

AOF 重放流程（启动时）:
读取 kkcache.aof
    │  逐行解析 RESP 命令
    ▼
Commands.get(cmd).exec(cache, args)   ← 重新执行每条命令
    │
    ▼
内存状态恢复完成
```

### 测试流程（v4.0）

```bash
# 写入数据
SET persistent_key hello_world
LPUSH mylist a b c
HSET user name kim age 30

# 重启服务器（Ctrl+C 后重新 mvn spring-boot:run）

# 验证数据恢复
GET persistent_key
# 期望: "hello_world"
LRANGE mylist 0 -1
# 期望: 1) "c"  2) "b"  3) "a"
HGETALL user
# 期望: name, kim, age, 30

# AOF 重写
BGREWRITEAOF
# 期望: Background append only file rewriting started
```

---

## v5.0 — 主从复制

### 目标
支持一主多从架构，从节点实时同步主节点数据，提升读吞吐量和可用性。

### 核心概念类比
主从复制就像**总部和分店**：总部（主节点）处理所有写操作，并把每笔操作同步给各分店（从节点）。顾客可以去任意分店查询，但只能去总部下单。

### 功能列表

#### 5.1 主节点（Master）
- 维护已连接的从节点列表（`replicaList`）
- 每次写命令执行后，将 RESP 命令广播给所有从节点
- 支持命令：`REPLICAOF NO ONE`（切换为主节点）

#### 5.2 从节点（Replica）
- 启动时向主节点发送 `REPLCONF` 握手，请求全量同步
- 全量同步：主节点发送当前内存快照（RDB 格式或逐条命令）
- 增量同步：之后持续接收主节点的写命令并执行
- 支持命令：`REPLICAOF host port`

#### 5.3 复制流程图

```
从节点启动
    │  REPLICAOF 127.0.0.1 6379
    ▼
连接主节点，发送 REPLCONF listening-port <port>
    │
    ▼
主节点发送全量快照（所有当前 key-value）
    │
    ▼
从节点加载快照，内存与主节点一致
    │
    ▼
主节点每次写操作 → 广播给从节点 → 从节点执行
```

### 测试流程（v5.0）

```bash
# 启动主节点（端口 6379）
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=6379"

# 启动从节点（端口 6380）
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=6380"

# 在从节点执行
redis-cli -p 6380
REPLICAOF 127.0.0.1 6379
# 期望: OK

# 在主节点写入
redis-cli -p 6379
SET replicated_key hello_replica

# 在从节点验证同步
redis-cli -p 6380
GET replicated_key
# 期望: "hello_replica"

# 验证从节点只读
redis-cli -p 6380
SET readonly_test fail
# 期望: (error) READONLY You can't write against a read only replica
```

---

## 未来开发计划（v6.0+）

### v6.0 — 集群模式（Cluster）
将数据按 key 的哈希槽（0-16383）分片到多个节点，每个节点负责一部分槽位。
- 实现 CRC16 哈希槽计算
- 节点间通过 Gossip 协议交换集群状态
- 客户端请求路由：命中本节点直接处理，否则返回 `MOVED` 重定向
- 支持命令：`CLUSTER INFO`、`CLUSTER NODES`

### v6.1 — 发布订阅（Pub/Sub）
类比广播电台：发布者向频道发消息，所有订阅该频道的客户端都能收到。
- 维护 `channel → List<ChannelHandlerContext>` 订阅关系表
- 支持命令：`SUBSCRIBE`、`UNSUBSCRIBE`、`PUBLISH`
- 消息推送：发布时遍历订阅者列表，逐一推送

### v6.2 — 事务支持（Transaction）
类比"打包快递"：先把多个操作放进购物车（MULTI），确认后一次性提交（EXEC），中途可以取消（DISCARD）。
- 每个连接维护一个命令队列
- `MULTI` 开启事务，后续命令入队而不立即执行
- `EXEC` 原子执行队列中所有命令
- `DISCARD` 清空队列，退出事务
- `WATCH` 实现乐观锁：监听 key，若在 EXEC 前被修改则事务失败

### v6.3 — Lua 脚本（Scripting）
内嵌 Lua 解释器（如 LuaJ），允许客户端上传并执行脚本，实现原子性复合操作。
- 支持命令：`EVAL script numkeys key [key ...] arg [arg ...]`、`EVALSHA`、`SCRIPT LOAD`

### v6.4 — 慢查询日志 & 监控
- 记录执行时间超过阈值的命令（`slowlog-log-slower-than`）
- 支持命令：`SLOWLOG GET`、`SLOWLOG LEN`、`SLOWLOG RESET`
- 暴露 Prometheus metrics 端点，集成 Spring Boot Actuator

### v6.5 — 认证与 ACL
- `AUTH password` 基础密码认证
- ACL（访问控制列表）：为不同用户配置可执行的命令和可访问的 key 前缀
- 支持命令：`ACL SETUSER`、`ACL GETUSER`、`ACL LIST`