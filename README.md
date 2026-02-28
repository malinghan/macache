# MaCache

基于 Java 实现的 Redis 协议兼容缓存服务器，使用 Netty 作为网络层，Spring Boot 作为应用容器。

## 技术栈

- Java 17
- Spring Boot 4.0.3
- Netty 4.1.104.Final
- 监听端口：6379

## 架构

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

## 快速开始

```bash
# 构建并运行
./mvnw spring-boot:run

# 使用 redis-cli 连接
redis-cli -p 6379
```

## 支持的命令

### 通用
| 命令 | 说明 |
|------|------|
| PING | 连通性测试，返回 PONG |
| HELLO | RESP 协议握手 |
| INFO | 返回服务器信息 |
| COMMAND | 返回所有命令名列表 |

### String
| 命令 | 说明 |
|------|------|
| SET / GET | 设置/获取 key-value |
| STRLEN | 返回字符串长度 |
| DEL / EXISTS | 删除/判断 key |
| INCR / DECR | 整数自增/自减 |
| MSET / MGET | 批量设置/获取 |

### List
`LPUSH` `RPUSH` `LPOP` `RPOP` `LLEN` `LINDEX` `LRANGE`

### Set
`SADD` `SMEMBERS` `SREM` `SCARD` `SPOP` `SISMEMBER`

### Hash
`HSET` `HGET` `HGETALL` `HLEN` `HDEL` `HEXISTS` `HMGET`

### ZSet
`ZADD` `ZCARD` `ZSCORE` `ZREM` `ZRANK` `ZCOUNT`

### TTL（v3.0）
`EXPIRE` `PEXPIRE` `TTL` `PTTL` `PERSIST`

## 扩展命令

1. 在 `command/` 包下创建新类，实现 `Command` 接口
2. 实现 `name()` 返回命令名（大写）
3. 实现 `exec()` 调用 `MaCache` 对应方法并返回 `Reply`
4. 在 `Commands` 的 `static {}` 块中注册：`register(new XxxCommand())`

## 已知限制

- 无持久化：重启后数据丢失（v4.0 计划实现 AOF）
- 无认证：不支持 AUTH 命令
- 单实例：不支持集群或主从复制（v5.0 计划实现主从）
