# MaCache 设计文档

## 项目概述

MaCache 是一个基于 Java 实现的 Redis 协议兼容缓存服务器，使用 Netty 作为网络层，Spring Boot 作为应用容器。它模拟 Redis 的 RESP（Redis Serialization Protocol）协议，支持五种核心数据类型的常用命令。

- **版本**: 0.0.1-SNAPSHOT
- **Java**: 17
- **Spring Boot**: 3.3.0
- **Netty**: 4.1.104.Final
- **监听端口**: 6379

---

## 架构设计

### 整体架构

```
Client (redis-cli / any Redis client)
        │  RESP Protocol (TCP)
        ▼
┌─────────────────────────────────────┐
│           MaCacheServer             │  Netty NIO Server (port 6379)
│  ┌──────────────┐  ┌─────────────┐  │
│  │MaCacheDecoder│→ │MaCacheHandler│  │  Netty Pipeline
│  └──────────────┘  └──────┬──────┘  │
└─────────────────────────── │ ────────┘
                             │
                    ┌────────▼────────┐
                    │    Commands     │  命令注册表 (静态 Map)
                    └────────┬────────┘
                             │ Command.exec()
                    ┌────────▼────────┐
                    │    MaCache      │  内存数据存储
                    └─────────────────┘
```

### 启动流程

Spring Boot 启动后，`MaApplicationListener` 监听 `ApplicationReadyEvent`，依次调用所有 `MaPlugin` 实现的 `init()` 和 `startup()`。`MaCacheServer` 实现了 `MaPlugin` 接口，在 `startup()` 中启动 Netty 服务器。

```
SpringBoot Start
    → ApplicationReadyEvent
    → MaApplicationListener.onApplicationEvent()
    → MaCacheServer.init()   // 创建 bossGroup / workerGroup
    → MaCacheServer.startup() // 绑定端口，阻塞等待连接
```

---

## 核心模块

### 1. 网络层

#### MaCacheServer
- 实现 `MaPlugin` 接口，作为 Spring Bean 注册
- 使用 Netty NIO 模型：1 个 boss 线程 + 16 个 worker 线程
- TCP 参数：`SO_BACKLOG=128`、`TCP_NODELAY`、`SO_KEEPALIVE`、`SO_REUSEADDR`、`PooledByteBufAllocator`

#### MaCacheDecoder
- 继承 `ByteToMessageDecoder`，将 TCP 字节流解码为字符串
- 简单实现：读取所有可读字节，转为 UTF-8 字符串，传递给 Handler

#### MaCacheHandler
- 继承 `SimpleChannelInboundHandler<String>`
- 解析 RESP 协议数组格式，提取命令名称
- 通过 `Commands.get(cmd)` 查找命令实现并执行
- 将 `Reply` 对象编码为 RESP 格式写回客户端

### 2. 协议层（RESP 编码）

MaCacheHandler 支持五种 RESP 响应类型的编码：

| ReplyType     | RESP 前缀 | 示例                        |
|---------------|-----------|-----------------------------|
| SIMPLE_STRING | `+`       | `+OK\r\n`                   |
| ERROR         | `-`       | `-ERR message\r\n`          |
| INT           | `:`       | `:42\r\n`                   |
| BULK_STRING   | `$`       | `$5\r\nhello\r\n`           |
| ARRAY         | `*`       | `*2\r\n$3\r\nfoo\r\n...`    |

### 3. 命令层

#### Command 接口
所有命令实现此接口：

```java
public interface Command {
    String name();                              // 命令名，如 "SET"
    Reply<?> exec(MaCache cache, String[] args); // 执行逻辑
}
```

接口提供了一组默认方法用于从 RESP 参数数组中提取参数：
- `getKey(args)` — 获取第一个 key（args[4]）
- `getVal(args)` — 获取第一个 value（args[6]）
- `getParams(args)` — 获取多个参数
- `getHkeys/getHvals` — 获取 hash 的 field/value 对

#### Commands 注册表
静态 `LinkedHashMap` 存储所有命令，应用启动时通过 `static {}` 块完成注册。

### 4. 存储层

#### MaCache
纯内存存储，核心结构为：

```java
Map<String, CacheEntry<?>> map = new HashMap<>();
```

`CacheEntry<T>` 是泛型包装类，`T` 根据数据类型不同而不同：

| 数据类型 | Java 实现类              |
|----------|--------------------------|
| String   | `String`                 |
| List     | `LinkedList<String>`     |
| Set      | `LinkedHashSet<String>`  |
| Hash     | `LinkedHashMap<String, String>` |
| ZSet     | `LinkedHashSet<ZsetEntry>` |

`ZsetEntry` 包含 `value: String` 和 `score: double` 两个字段。

---

## 支持的命令

### 通用命令
| 命令    | 说明               |
|---------|--------------------|
| PING    | 连通性测试，返回 PONG |
| INFO    | 返回服务器信息      |
| COMMAND | 返回所有命令名列表  |

### String 类型
| 命令   | 说明                        |
|--------|-----------------------------|
| SET    | 设置 key-value              |
| GET    | 获取 value                  |
| STRLEN | 返回字符串长度              |
| DEL    | 删除一个或多个 key          |
| EXISTS | 判断 key 是否存在           |
| INCR   | 整数值自增 1                |
| DECR   | 整数值自减 1                |
| MSET   | 批量设置                    |
| MGET   | 批量获取                    |

### List 类型
| 命令   | 说明                        |
|--------|-----------------------------|
| LPUSH  | 从左侧插入元素              |
| RPUSH  | 从右侧插入元素              |
| LPOP   | 从左侧弹出元素              |
| RPOP   | 从右侧弹出元素              |
| LLEN   | 返回列表长度                |
| LINDEX | 按索引获取元素              |
| LRANGE | 获取指定范围的元素          |

### Set 类型
| 命令      | 说明                    |
|-----------|-------------------------|
| SADD      | 添加成员                |
| SMEMBERS  | 返回所有成员            |
| SREM      | 删除成员                |
| SCARD     | 返回成员数量            |
| SPOP      | 随机弹出成员            |
| SISMEMBER | 判断成员是否存在        |

### Hash 类型
| 命令    | 说明                        |
|---------|-----------------------------|
| HSET    | 设置 field-value            |
| HGET    | 获取 field 的值             |
| HGETALL | 获取所有 field 和 value     |
| HLEN    | 返回 field 数量             |
| HDEL    | 删除 field                  |
| HEXISTS | 判断 field 是否存在         |
| HMGET   | 批量获取多个 field 的值     |

### ZSet 类型
| 命令   | 说明                            |
|--------|---------------------------------|
| ZADD   | 添加成员及分数                  |
| ZCARD  | 返回成员数量                    |
| ZSCORE | 获取成员分数                    |
| ZREM   | 删除成员                        |
| ZRANK  | 获取成员排名（按分数升序）      |
| ZCOUNT | 统计分数范围内的成员数量        |

---

## 扩展机制

### 添加新命令
1. 在 `command/` 包下创建新类，实现 `Command` 接口
2. 实现 `name()` 返回命令名（大写）
3. 实现 `exec()` 调用 `MaCache` 对应方法并返回 `Reply`
4. 在 `Commands.initCommands()` 中调用 `register(new XxxCommand())`

### 添加新插件
实现 `MaPlugin` 接口并注册为 Spring Bean，`MaApplicationListener` 会自动发现并管理其生命周期。

---

## 已知限制

- **无持久化**：所有数据存储在 JVM 堆内存，重启后丢失
- **无过期机制**：不支持 TTL / EXPIRE 命令
- **无认证**：不支持 AUTH 命令
- **单实例**：不支持集群或主从复制
- **解码简化**：`MaCacheDecoder` 未处理 TCP 粘包/拆包，依赖客户端单次发送完整命令
- **类型安全**：`MaCache` 内部使用强制类型转换，混用不同类型操作同一 key 会抛出 `ClassCastException`