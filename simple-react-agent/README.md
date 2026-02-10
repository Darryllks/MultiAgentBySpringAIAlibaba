# Simple React Agent

基于 Spring AI Alibaba Agent Framework 的简单 ReAct Agent 实现。

## 📋 概述

这是一个展示基础 ReAct (Reasoning and Acting) 模式的示例项目，演示了如何使用 Spring AI Alibaba 构建简单的 AI 智能体。

## 🏗️ 项目结构

```
simple-react-agent/
├── src/main/java/com/lks/graphAgent/
│   ├── SimpleReactAgentApplication.java    # 应用启动类
│   ├── config/
│   │   ├── RedisConfig.java                # Redis 配置
│   │   └── SimpleReactAgentWithRedisSaverConfig.java  # Agent 配置
│   ├── controller/
│   │   └── Controller.java                 # REST 控制器
│   └── documents/                          # 技术文档
│       ├── ReactAgent_详细解析.md
│       ├── ReactAgent_Graph_工作流程详解.md
│       └── StateGraph编译过程详解.md
└── src/main/resources/
    └── application.yaml                    # 应用配置
```

## 🚀 快速开始

### 1. 环境准备

确保安装了以下依赖：
- JDK 21+
- Maven 3.8+
- Redis 服务器（可选）

### 2. 配置环境变量

```bash
export AI_DASHSCOPE_API_KEY=your_dashscope_api_key_here
```

Windows PowerShell:
```powershell
$env:AI_DASHSCOPE_API_KEY="your_dashscope_api_key_here"
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8001` 启动。

## 🔧 核心组件

### 1. Redis 配置 (`RedisConfig.java`)

配置 Redis 连接和 Redisson 客户端：

```java
@Configuration
@EnableConfigurationProperties
public class RedisConfig {
    // Redis 连接池配置
    // Redisson 客户端配置
}
```

### 2. Agent 配置 (`SimpleReactAgentWithRedisSaverConfig.java`)

配置带有 Redis 状态保存的 ReactAgent：

```java
@Bean
public ReactAgent reactAgent(ChatModel chatModel) {
    return ReactAgent.builder()
        .model(chatModel)
        .systemPrompt("你是一个有用的助手...")
        .saver(redisSaver)  // Redis 状态保存器
        .build();
}
```

### 3. 控制器 (`Controller.java`)

提供 RESTful API 接口：

```java
@RestController
@RequestMapping("/api")
public class Controller {
    
    @PostMapping("/chat")
    public Mono<String> chat(@RequestBody Map<String, String> request) {
        // 处理聊天请求
    }
    
    @GetMapping("/state/{threadId}")
    public Mono<Map<String, Object>> getState(@PathVariable String threadId) {
        // 获取指定线程的状态
    }
}
```

## 📊 API 接口

### 1. 聊天接口

```http
POST /api/chat
Content-Type: application/json

{
    "message": "你好，帮我解释一下量子计算"
}
```

### 2. 获取状态

```http
GET /api/state/{threadId}
```

### 3. 流式聊天（如果实现）

```http
POST /api/chat/stream
Accept: text/event-stream
Content-Type: application/json

{
    "message": "详细解释机器学习"
}
```

## 🛠️ 技术特性

### 1. 状态持久化

使用 Redis 保存对话状态，支持：
- 会话状态管理
- 断点续聊
- 多用户隔离

### 2. 响应式编程

基于 Spring WebFlux 实现：
- 非阻塞 I/O
- 高并发处理
- 流式响应支持

### 3. 配置管理

通过 `application.yaml` 管理配置：

```yaml
server:
  port: 8001

spring:
  application:
    name: simple-react-agent
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
  data:
    redis:
      host: localhost
      port: 6379
```

## 📚 学习资源

### 相关文档

- [ReactAgent 详细解析](./documents/ReactAgent_详细解析.md) - 核心实现原理
- [Graph 工作流程详解](./documents/ReactAgent_Graph_工作流程详解.md) - 状态图机制
- [StateGraph 编译过程](./documents/StateGraph编译过程详解.md) - 图编译流程

### 关键概念

1. **ReAct 模式**：推理-行动循环
2. **StateGraph**：状态驱动的执行图
3. **OverAllState**：全局状态管理
4. **RunnableConfig**：运行时配置

## 🔧 开发指南

### 添加新工具

```java
@Component
public class CustomTool {
    
    @Tool(name = "custom_tool", description = "自定义工具描述")
    public String execute(@P("参数描述") String param) {
        // 工具实现逻辑
        return "工具执行结果";
    }
}
```

### 自定义拦截器

```java
@Component
public class CustomInterceptor implements ToolInterceptor {
    
    @Override
    public ToolResponseMessage intercept(ToolInterceptorChain chain, ToolCall toolCall) {
        // 拦截逻辑
        return chain.proceed(toolCall);
    }
}
```

## 🧪 测试

运行单元测试：

```bash
mvn test
```

运行集成测试：

```bash
mvn verify
```

## 📈 性能优化

### 1. 连接池配置

调整 Redis 连接池大小：

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

### 2. 缓存策略

合理设置状态缓存过期时间：

```java
@Cacheable(value = "agent-states", key = "#threadId", ttl = 3600)
public Map<String, Object> getState(String threadId) {
    // ...
}
```

## 📄 许可证

MIT License