# MultiAgentBySpringAIAlibaba

基于 Spring AI Alibaba Agent Framework 的多智能体系统示例项目，展示了两种不同类型的 AI 智能体实现：简单 React Agent 和深度研究 Agent。

## 📋 项目概述

这是一个演示如何使用 Spring AI Alibaba 构建不同类型 AI 智能体的示例项目。项目包含两个独立的模块，分别展示了基础的 ReAct 模式和复杂的多代理协作系统。

## 🏗️ 项目结构

```
MultiAgentBySpringAIAlibaba/
├── simple-react-agent/           # 简单 React Agent 模块
│   ├── src/main/java/
│   │   └── com/lks/graphAgent/
│   │       ├── SimpleReactAgentApplication.java    # 应用启动类
│   │       ├── config/                            # 配置类
│   │       ├── controller/                        # 控制器
│   │       └── documents/                         # 技术文档
│   └── src/main/resources/
│       └── application.yaml                       # 应用配置
├── deepResearchAgent/            # 深度研究 Agent 模块
│   ├── src/main/java/
│   │   └── com/lks/agent/
│   │       ├── DeepRearchAgentApplication.java    # 应用启动类
│   │       ├── Agents/                           # Agent 实现
│   │       ├── Loader/                           # 加载器
│   │       └── config/                           # 配置类
│   └── src/main/resources/
│       ├── application.yaml                      # 应用配置
│       └── mcp-servers-config.json               # MCP 服务器配置
└── pom.xml                                       # 父项目配置
```

## 🚀 技术栈

- **Java**: 21
- **Spring Boot**: 3.5.7
- **Spring AI**: 1.1.0
- **Spring AI Alibaba**: 1.1.0.0
- **Maven**: 项目构建工具
- **Redis**: 状态持久化（simple-react-agent）
- **DashScope**: 阿里云百炼大模型 API

## 🔧 环境要求

- JDK 21 或更高版本
- Maven 3.8+
- Redis 服务器（可选，用于 simple-react-agent）
- 阿里云百炼 API Key

## ⚙️ 环境变量配置

在运行项目前，需要设置以下环境变量：

```bash
# 阿里云百炼 API Key（必需）
export AI_DASHSCOPE_API_KEY=your_dashscope_api_key_here

# Redis 配置（simple-react-agent 模块）
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379
```

Windows PowerShell:
```powershell
$env:AI_DASHSCOPE_API_KEY="your_dashscope_api_key_here"
```

## 📦 模块介绍

### 1. simple-react-agent（简单 React Agent）

基于 Spring AI Alibaba Agent Framework 的基础 ReAct 模式实现。

**特性：**
- 基础的 Reasoning and Acting 模式
- Redis 状态持久化支持
- WebFlux 响应式编程
- 简洁的 Agent 实现

**启动命令：**
```bash
cd simple-react-agent
mvn spring-boot:run
```

**访问地址：** `http://localhost:8001`

### 2. deepResearchAgent（深度研究 Agent）

高级研究代理系统，具备复杂的多代理协作能力。

**核心特性：**
- **多代理架构**：主代理协调多个专用子代理
- **MCP 集成**：支持 Model Context Protocol 客户端
- **智能拦截器系统**：
  - `LargeResultEvictionInterceptor`：大结果自动保存
  - `FilesystemInterceptor`：文件系统访问控制
  - `TodoListInterceptor`：任务进度管理
  - `ContextEditingInterceptor`：上下文自动压缩
  - `ToolRetryInterceptor`：工具调用重试机制
- **丰富钩子机制**：
  - `SummarizationHook`：对话历史自动摘要
  - `HumanInTheLoopHook`：人类审批流程
  - `ToolCallLimitHook`：工具调用次数限制
- **专业子代理**：
  - `research-agent`：深度研究专用代理
  - `critique-agent`：报告质量评审代理

**启动命令：**
```bash
cd deepResearchAgent
mvn spring-boot:run
```

**访问地址：** `http://localhost:8080/chatui/index.html`

## 🛠️ MCP 服务器配置

deepResearchAgent 模块集成了以下 MCP 服务器：

### ArXiv MCP Server
```json
{
  "command": "uv",
  "args": ["tool", "run", "arxiv-mcp-server", "--storage-path", "./papers-storage"]
}
```

### Jina MCP Tools
```json
{
  "command": "npx.cmd",
  "args": ["jina-mcp-tools", "--transport", "stdio", "--tokens-per-page", "15000"],
  "env": {
    "JINA_API_KEY": "your_jina_api_key"
  }
}
```

## 📚 核心概念

### ReAct 模式
ReAct (Reasoning and Acting) 是一种让 AI 模型交替进行推理和行动的方法：

1. **Reasoning（推理）**：模型分析问题并制定行动计划
2. **Acting（行动）**：执行具体的工具调用或操作
3. **Observation（观察）**：观察行动结果
4. **循环**：基于观察结果继续推理和行动

### 拦截器（Interceptors）
用于在 Agent 执行流程的关键节点插入自定义逻辑：
- 工具调用前后的处理
- 结果大小控制
- 错误重试机制
- 上下文管理

### 钩子（Hooks）
提供更细粒度的控制点：
- Agent 执行前后
- 模型调用前后
- 人类介入控制
- 自动摘要生成

## 🔧 开发指南

### 项目构建
```bash
# 构建整个项目
mvn clean install

# 构建特定模块
mvn clean install -pl simple-react-agent
mvn clean install -pl deepResearchAgent
```

### 代码结构说明

#### Agent 核心类继承关系
```
Agent (抽象类)
└── BaseAgent (抽象类)
    └── ReactAgent (具体实现)
```

#### 状态管理
- `OverAllState`：维护全局状态
- `StateGraph`：定义执行流程图
- `CompiledGraph`：编译后的可执行图

### 自定义 Agent 开发

1. **创建 Agent 类**
```java
public class CustomAgent extends ReactAgent {
    // 自定义实现
}
```

2. **配置拦截器**
```java
.interceptors(
    customInterceptor1,
    customInterceptor2
)
```

3. **添加钩子**
```java
.hooks(
    customHook1,
    customHook2
)
```

## 📖 技术文档

项目包含详细的中文技术文档：

- [`ReactAgent_详细解析.md`](./simple-react-agent/src/main/java/com/lks/graphAgent/documents/ReactAgent_详细解析.md) - ReactAgent 核心实现详解
- [`ReactAgent_Graph_工作流程详解.md`](./simple-react-agent/src/main/java/com/lks/graphAgent/documents/ReactAgent_Graph_工作流程详解.md) - 状态图工作机制
- [`StateGraph编译过程详解.md`](./simple-react-agent/src/main/java/com/lks/graphAgent/documents/StateGraph编译过程详解.md) - 图编译流程

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) - 提供强大的 Agent Framework
- [阿里云百炼](https://help.aliyun.com/zh/model-studio/) - 提供优质的 AI 模型服务
- [Model Context Protocol](https://modelcontextprotocol.io/) - 标准化的工具协议

---

**Happy Coding!** 💻✨