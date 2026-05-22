# 🏭 WES Task Scheduler Simulator

**仓储执行系统 - 任务调度引擎模拟器**

> 一个基于 Java 17 + Spring Boot 3.2 的 WES (Warehouse Execution System) 任务调度引擎模拟器，用于演示仓储自动化场景下的核心调度逻辑。

---

## ✨ 核心特性

### 🎯 优先级队列调度
- 多级优先级：`URGENT > HIGH > NORMAL > LOW`
- 基于 `PriorityBlockingQueue` 实现，紧急任务优先分配

### 🤖 智能设备分配
- 10 台模拟设备：输送线(×2)、AGV(×3)、四穿车(×2)、提升机(×2)、机械臂(×1)
- **四层分配策略**：同区域优先 → 设备类型匹配 → 效率最高 → 兜底随机
- 每台设备有独立的效率系数，影响任务执行耗时

### 💾 Redis 持久化
- 统计信息自动持久化到 Redis，支持重启恢复
- 基于 Spring Data Redis + JSON 序列化

### 🔌 WebSocket 实时推送
- 任务分配/完成/超时等事件通过 WebSocket 实时推送到前端
- 浏览器控制台实时展示调度全过程

### 📊 可视化监控
- 暗色主题 Web 控制台
- 设备实时状态仪表盘（空闲/忙碌/故障）
- 事件日志滚动条
- 支持批量生成测试任务验证调度逻辑

---

## 🚀 快速启动

### 前置条件
- Java 17+
- Redis 8+
- Maven 3.6+

### 启动步骤

```bash
# 1. 启动 Redis
redis-server

# 2. 编译
mvn clean package -DskipTests

# 3. 运行
java -jar ces-demo-web/target/ces-demo-web-1.0.0.jar

# 4. 打开浏览器
open http://localhost:8080
```

### 测试调度
1. 打开 Web 控制台：http://localhost:8080
2. 点击「生成10个测试任务」，观察任务自动分配和执行
3. 查看设备状态变化和实时事件日志

---

## 🏗️ 项目架构

```
ces-demo/
├── ces-core/                  # 调度引擎核心
│   └── src/main/java/.../
│       ├── model/             # 领域模型
│       │   ├── Task.java              # 任务模型
│       │   ├── TaskPriority.java      # 任务优先级枚举
│       │   ├── TaskStatus.java        # 任务状态机
│       │   └── Worker.java            # 设备模型
│       ├── scheduler/
│       │   └── TaskSchedulerEngine.java  # 调度引擎核心
│       └── config/
│           ├── WESConfig.java         # 设备初始化配置
│           └── RedisConfig.java       # Redis 序列化配置
├── ces-api/                   # REST API + WebSocket
│   └── src/main/java/.../
│       ├── controller/
│       │   └── WESController.java     # REST 接口
│       ├── dto/
│       │   └── DispatchSnapshot.java  # 状态快照 DTO
│       └── websocket/
│           ├── WebSocketConfig.java   # WebSocket 配置
│           └── DispatchWebSocketHandler.java  # 事件推送
└── ces-demo-web/              # 启动入口 + Web 控制台
    └── src/main/
        ├── java/.../CESDemoApplication.java
        ├── resources/
        │   ├── application.properties
        │   └── templates/index.html   # 前端监控面板
```

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| Java 17 | 开发语言 |
| Spring Boot 3.2 | 项目框架 |
| Spring Web | REST API |
| Spring Data Redis | 任务/设备/统计持久化 |
| WebSocket | 实时事件推送 |
| Maven 多模块 | 工程架构 (core/api/web) |
| PriorityBlockingQueue | 核心调度队列 |
| ConcurrentHashMap | 并发任务池 |
| ScheduledExecutorService | 定时调度循环 |
| Thymeleaf | 前端模板 |
| Jackson | JSON 序列化 |

---

## 🧠 调度策略详解

### 设备选择算法
```
1. 同区域优先  →  任务源库位所在区域的设备优先
2. 类型匹配    →  PICK任务→AGV, PUT任务→输送线, REPLENISH→四穿车
3. 效率最高    →  同类型设备中选择效率最高的
4. 兜底随机    →  任意空闲设备
```

### 任务状态机
```
WAITING → QUEUED → PROCESSING → COMPLETED
                                → FAILED
                                → TIMEOUT (≥30s)
```

---

## 📄 License

MIT
