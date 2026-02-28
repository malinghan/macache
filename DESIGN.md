# MaCache 设计文档

## 项目概述

MaCache 是一个基于 Java 实现的 Redis 协议兼容缓存服务器，使用 Netty 作为网络层，Spring Boot 作为应用容器。它模拟 Redis 的 RESP（Redis Serialization Protocol）协议，支持五种核心数据类型的常用命令。

- **版本**: v3.0
- **Java**: 17
- **Spring Boot**: 4.0.3
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
    → MaCacheServer.init()    // 创建 bossGroup / workerGroup
    → MaCacheServer.startup() // 绑定端口，阻塞等待连接
```

---

## 设计模式分析

### 1. 命令模式（Command Pattern）

**涉及类**：`Command`、`SetCommand`、`GetCommand`、`LpushCommand` 等所有命令类、`MaCacheHandler`

**结构**：

```
«interface»
Command
  + name(): String
  + exec(cache, args): Reply<?>
       ▲
       │ implements
  ┌────┴────┬──────────┬──────────┐
SetCommand GetCommand LpushCommand  ...（38 个命令类）
```

**体现**：

- `Command` 接口将"请求"封装为对象，每个命令类代表一个具体操作
- `MaCacheHandler` 是调用者（Invoker），只知道 `Command` 接口，不关心具体实现
- `MaCache` 是接收者（Receiver），实际执行数据操作
- `Commands` 是命令注册表，负责命令的存储与查找

**优势**：新增命令只需新建一个类并注册，`MaCacheHandler` 无需任何修改，完全符合开闭原则。

---

### 2. 注册表模式（Registry Pattern）

**涉及类**：`Commands`

**结构**：

```java
public class Commands {
    static LinkedHashMap<String, Command> map = new LinkedHashMap<>();
    static { register(new SetCommand()); ... }

    public static Command get(String name) { return map.get(name.toUpperCase()); }
}
```

**体现**：

- `Commands` 是一个静态注册表，在类加载时通过 `static {}` 块完成所有命令的注册
- 通过命令名（字符串）查找对应的命令实现，实现了名称到对象的映射
- 使用 `LinkedHashMap` 保证注册顺序，便于 `COMMAND` 命令按注册顺序返回命令列表

**与工厂模式的区别**：注册表不负责创建对象，只负责存储和查找已创建的对象。

---

### 3. 策略模式（Strategy Pattern）

**涉及类**：`Command` 接口及其所有实现类

**体现**：

- `Command.exec()` 定义了"执行一条缓存命令"的算法骨架
- 每个命令类（`SetCommand`、`GetCommand` 等）是一个具体策略，封装了不同的执行逻辑
- `MaCacheHandler` 在运行时根据命令名动态选择策略，行为可以在不修改调用方的情况下切换

命令模式与策略模式在结构上相似，区别在于意图：命令模式强调"请求的封装与解耦"，策略模式强调"算法的可替换性"。MaCache 中两者同时体现。

---

### 4. 模板方法模式（Template Method Pattern）

**涉及类**：`Command` 接口中的 `default` 方法

**结构**：

```java
public interface Command {
    String name();
    Reply<?> exec(MaCache cache, String[] args);  // 抽象方法（子类实现）

    // 模板方法：提供通用的参数提取逻辑
    default String getKey(String[] args)    { return args[1]; }
    default String getVal(String[] args)    { return args[2]; }
    default String[] getParams(String[] args) { ... }
}
```

**体现**：

- `Command` 接口通过 `default` 方法定义了参数提取的通用算法
- 子类（各命令类）直接复用这些默认实现，无需重复编写参数解析逻辑
- 子类也可以覆盖默认方法以实现特殊的参数提取逻辑

Java 8+ 的接口 `default` 方法是模板方法模式在接口层面的现代实现。

---

### 5. 插件模式（Plugin Pattern）/ 生命周期钩子

**涉及类**：`MaPlugin`、`MaCacheServer`、`MaApplicationListener`

**结构**：

```
«interface»
MaPlugin
  + init()
  + startup()
       ▲
       │ implements
  MaCacheServer

MaApplicationListener
  - plugins: List<MaPlugin>   ← Spring 自动注入所有实现类
  + onApplicationEvent()
      → plugins.forEach(init + startup)
```

**体现**：

- `MaPlugin` 定义了组件的生命周期契约（`init` → `startup`）
- `MaApplicationListener` 通过 Spring 的依赖注入自动收集所有 `MaPlugin` 实现，统一驱动生命周期
- 新增插件只需实现 `MaPlugin` 并注册为 Spring Bean，无需修改 `MaApplicationListener`

这是插件模式与观察者模式（Spring 的 `ApplicationListener`）的组合应用。

---

### 6. 观察者模式（Observer Pattern）

**涉及类**：`MaApplicationListener`、Spring 的 `ApplicationReadyEvent`

**体现**：

- `MaApplicationListener` 实现 `ApplicationListener<ApplicationReadyEvent>`，订阅 Spring 容器的就绪事件
- Spring 容器（发布者）在启动完成后发布 `ApplicationReadyEvent`
- `MaApplicationListener`（观察者）收到事件后触发所有插件的启动流程

这是 Spring 框架内置的观察者模式，MaCache 借助它实现了与 Spring 生命周期的解耦。

---

### 7. 值对象模式（Value Object Pattern）

**涉及类**：`Reply<T>`、`ReplyType`

**结构**：

```java
public class Reply<T> {
    private final ReplyType type;
    private final T data;

    // 只有静态工厂方法，无 setter
    public static Reply<String> ok()              { ... }
    public static Reply<String> error(String msg) { ... }
    public static Reply<Long>   integer(long val) { ... }
    public static Reply<String> bulkString(String val) { ... }
    public static Reply<List<String>> array(List<String> val) { ... }
    public static Reply<String> nil()             { ... }
}
```

**体现**：

- `Reply<T>` 是不可变对象，字段均为 `final`，只通过静态工厂方法创建
- 使用泛型封装不同类型的响应数据，类型安全
- 静态工厂方法语义清晰（`Reply.ok()`、`Reply.nil()`），比构造函数更具表达力

---

### 8. 泛型包装模式（Generic Wrapper Pattern）

**涉及类**：`CacheEntry<T>`

**体现**：

- `CacheEntry<T>` 用泛型统一包装五种不同类型的数据（`String`、`LinkedList`、`LinkedHashSet`、`LinkedHashMap`、`TreeSet`）
- `MaCache.map` 的类型为 `Map<String, CacheEntry<?>>`，通过通配符实现异构存储
- 各操作方法内部通过 `instanceof` 检查 + 强制转换获取具体类型，并在类型不匹配时抛出 `WrongTypeException`

---

### 9. 责任链模式（Chain of Responsibility Pattern）

**涉及类**：Netty Pipeline（`MaCacheDecoder` → `StringEncoder` → `MaCacheHandler`）

**结构**：

```
TCP 字节流
    │
    ▼
MaCacheDecoder        ← 字节流 → 字符串（入站处理器）
    │
    ▼
MaCacheHandler        ← 字符串 → 命令执行 → Reply（入站处理器）
    │
    ▼
StringEncoder         ← String → 字节流（出站处理器）
    │
    ▼
TCP 响应
```

**体现**：

- Netty 的 `ChannelPipeline` 是责任链模式的典型实现
- 每个 Handler 只处理自己关心的部分，处理完后传递给下一个 Handler
- 入站（Inbound）和出站（Outbound）分别形成两条方向相反的责任链

---

## 设计模式汇总

| 设计模式 | 涉及类 | 作用 |
|----------|--------|------|
| 命令模式 | `Command` + 38 个命令类 + `MaCacheHandler` | 将请求封装为对象，解耦调用方与执行方 |
| 注册表模式 | `Commands` | 命令名到命令对象的映射，支持运行时查找 |
| 策略模式 | `Command` 接口 + 各命令实现 | 运行时动态选择命令执行算法 |
| 模板方法模式 | `Command` 接口的 `default` 方法 | 复用参数提取逻辑，子类只需关注业务 |
| 插件模式 | `MaPlugin` + `MaCacheServer` + `MaApplicationListener` | 统一管理组件生命周期，支持扩展 |
| 观察者模式 | `MaApplicationListener` + Spring `ApplicationReadyEvent` | 监听容器就绪事件，触发插件启动 |
| 值对象模式 | `Reply<T>` + `ReplyType` | 不可变响应封装，静态工厂方法语义清晰 |
| 泛型包装模式 | `CacheEntry<T>` | 统一存储异构数据类型 |
| 责任链模式 | Netty Pipeline | 分层处理网络数据的编解码与业务逻辑 |

---

## 核心模块

### 1. 网络层

#### MaCacheServer
- 实现 `MaPlugin` 接口，作为 Spring Bean 注册
- 使用 Netty NIO 模型：1 个 boss 线程 + 16 个 worker 线程

#### MaCacheDecoder
- 继承 `ByteToMessageDecoder`，将 TCP 字节流解码为字符串
- 包含完整性检查：RESP 数组格式需收到足够行数才传递，防止 TCP 拆包问题

#### MaCacheHandler
- 继承 `SimpleChannelInboundHandler<String>`，标注 `@Sharable` 支持多 channel 共享
- 解析 RESP 协议（数组格式和 inline 格式），提取命令名称
- 通过 `Commands.get(cmd)` 查找命令实现并执行
- 捕获 `WrongTypeException`，返回标准 WRONGTYPE 错误响应
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
    String name();                               // 命令名，如 "SET"
    Reply<?> exec(MaCache cache, String[] args); // 执行逻辑

    // 默认方法：通用参数提取
    default String getKey(String[] args)     { return args[1]; }
    default String getVal(String[] args)     { return args[2]; }
    default String[] getParams(String[] args) { ... }
}
```

#### Commands 注册表
静态 `LinkedHashMap` 存储所有命令，应用启动时通过 `static {}` 块完成注册。

### 4. 存储层

#### MaCache
纯内存存储，核心结构为：

```java
Map<String, CacheEntry<?>> map = new HashMap<>();
```

`CacheEntry<T>` 是泛型包装类，`T` 根据数据类型不同而不同：

| 数据类型 | Java 实现类                     |
|----------|---------------------------------|
| String   | `String`                        |
| List     | `LinkedList<String>`            |
| Set      | `LinkedHashSet<String>`         |
| Hash     | `LinkedHashMap<String, String>` |
| ZSet     | `TreeSet<ZsetEntry>`            |

v3.0 起，`CacheEntry` 增加 `expireAt` 字段支持 TTL，所有读操作通过 `getEntry()` 统一进行惰性过期检查。

---

## 支持的命令（v3.0）

### 通用命令
| 命令    | 说明                    |
|---------|-------------------------|
| PING    | 连通性测试，返回 PONG   |
| HELLO   | RESP 协议握手           |
| INFO    | 返回服务器信息          |
| COMMAND | 返回所有命令名列表      |

### String 类型
| 命令   | 说明               |
|--------|--------------------|
| SET    | 设置 key-value     |
| GET    | 获取 value         |
| STRLEN | 返回字符串长度     |
| DEL    | 删除一个或多个 key |
| EXISTS | 判断 key 是否存在  |
| INCR   | 整数值自增 1       |
| DECR   | 整数值自减 1       |
| MSET   | 批量设置           |
| MGET   | 批量获取           |

### List 类型
| 命令   | 说明               |
|--------|--------------------|
| LPUSH  | 从左侧插入元素     |
| RPUSH  | 从右侧插入元素     |
| LPOP   | 从左侧弹出元素     |
| RPOP   | 从右侧弹出元素     |
| LLEN   | 返回列表长度       |
| LINDEX | 按索引获取元素     |
| LRANGE | 获取指定范围的元素 |

### Set 类型
| 命令      | 说明             |
|-----------|------------------|
| SADD      | 添加成员         |
| SMEMBERS  | 返回所有成员     |
| SREM      | 删除成员         |
| SCARD     | 返回成员数量     |
| SPOP      | 随机弹出成员     |
| SISMEMBER | 判断成员是否存在 |

### Hash 类型
| 命令    | 说明                    |
|---------|-------------------------|
| HSET    | 设置 field-value        |
| HGET    | 获取 field 的值         |
| HGETALL | 获取所有 field 和 value |
| HLEN    | 返回 field 数量         |
| HDEL    | 删除 field              |
| HEXISTS | 判断 field 是否存在     |
| HMGET   | 批量获取多个 field 的值 |

### ZSet 类型
| 命令   | 说明                       |
|--------|----------------------------|
| ZADD   | 添加成员及分数             |
| ZCARD  | 返回成员数量               |
| ZSCORE | 获取成员分数               |
| ZREM   | 删除成员                   |
| ZRANK  | 获取成员排名（按分数升序） |
| ZCOUNT | 统计分数范围内的成员数量   |

### TTL 命令（v3.0 新增）
| 命令    | 说明                                    |
|---------|-----------------------------------------|
| EXPIRE  | 设置过期时间（秒）                      |
| PEXPIRE | 设置过期时间（毫秒）                    |
| TTL     | 返回剩余秒数，-1 永不过期，-2 key 不存在 |
| PTTL    | 返回剩余毫秒数                          |
| PERSIST | 移除过期时间                            |

---

## 扩展机制

### 添加新命令
1. 在 `command/` 包下创建新类，实现 `Command` 接口
2. 实现 `name()` 返回命令名（大写）
3. 实现 `exec()` 调用 `MaCache` 对应方法并返回 `Reply`
4. 在 `Commands` 的 `static {}` 块中调用 `register(new XxxCommand())`

### 添加新插件
实现 `MaPlugin` 接口并注册为 Spring Bean，`MaApplicationListener` 会自动发现并管理其生命周期。

---

## 已知限制

- **无持久化**：所有数据存储在 JVM 堆内存，重启后丢失（v4.0 计划实现 AOF）
- **无认证**：不支持 AUTH 命令
- **单实例**：不支持集群或主从复制（v5.0 计划实现主从）
- **解码简化**：`MaCacheDecoder` 的完整性检查基于行数统计，极端情况下仍可能有问题


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