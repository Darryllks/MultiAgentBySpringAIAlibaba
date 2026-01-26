# StateGraph编译为CompiledGraph的详细过程

## 概述

在Spring AI Alibaba图框架中，StateGraph是一个表示状态图的数据结构，包含节点和边。然而，StateGraph只是一个定义图结构的类，并不能直接执行。要使图能够执行，需要将其编译为CompiledGraph。本文档详细解释了从StateGraph到CompiledGraph的编译过程。

## 核心组件

### StateGraph类

StateGraph是图的定义类，主要包含：

- [nodes](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/StateGraph.java#L109-L109)：图中的所有节点集合
- [edges](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/StateGraph.java#L114-L114)：图中的所有边集合
- [keyStrategyFactory](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/StateGraph.java#L119-L119)：键策略工厂
- [stateSerializer](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/StateGraph.java#L129-L129)：状态序列化器

### CompiledGraph类

CompiledGraph是可执行的图实例，主要包含：

- [stateGraph](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L52-L52)：原始的StateGraph引用
- [compileConfig](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L57-L57)：编译配置
- [nodeFactories](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L62-L62)：节点动作工厂映射
- [edges](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L68-L68)：边值映射
- [keyStrategyMap](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L70-L70)：键策略映射

### CompileConfig类

CompileConfig包含编译时的各种配置选项：

- [recursionLimit](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompileConfig.java#L34-L34)：递归限制
- [saverConfig](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompileConfig.java#L28-L28)：保存器配置
- [interruptsBefore](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompileConfig.java#L45-L45)：执行前中断点
- [interruptsAfter](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompileConfig.java#L49-L49)：执行后中断点

## 编译过程详解

### 1. 初始化CompiledGraph构造函数

当调用[StateGraph.compile()](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/StateGraph.java#L343-L353)方法时，会创建一个新的CompiledGraph实例。编译过程的核心在[CompiledGraph](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L47-L299)构造函数中：

```java
protected CompiledGraph(StateGraph stateGraph, CompileConfig compileConfig) throws GraphStateException {
    this.maxIterations = compileConfig.recursionLimit();
    this.stateGraph = stateGraph;
    // ... 其他初始化代码
}
```

### 2. 提取键策略映射

编译过程首先从StateGraph的[keyStrategyFactory](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/StateGraph.java#L119-L119)中提取键策略映射：

```java
this.keyStrategyMap = stateGraph.getKeyStrategyFactory()
    .apply()
    .entrySet()
    .stream()
    .map(e -> Map.entry(e.getKey(), e.getValue()))
    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

### 3. 处理节点、边和配置

使用[ProcessedNodesEdgesAndConfig.process()](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/CompiledGraph.java#L649-L657)方法处理原始的StateGraph数据：

```java
this.processedData = ProcessedNodesEdgesAndConfig.process(stateGraph, compileConfig);
```

这个步骤特别重要，因为它处理子图（SubGraph）的情况，展开嵌套的图结构。

### 4. 合并子图的键策略

如果存在子图，则将子图的键策略合并到主图的键策略中：

```java
// 合并来自子图（StateGraph）的额外键和键策略
for (var entry : processedData.keyStrategyMap().entrySet()) {
    if (!this.keyStrategyMap.containsKey(entry.getKey())) {
        this.keyStrategyMap.put(entry.getKey(), entry.getValue());
    }
}
```

### 5. 验证中断点

检查所有中断点是否对应有效的节点：

```java
// 检查中断
for (String interruption : processedData.interruptsBefore()) {
    if (!processedData.nodes().anyMatchById(interruption)) {
        throw Errors.interruptionNodeNotExist.exception(interruption);
    }
}
```

### 6. 创建更新后的编译配置

基于处理后的数据创建最终的编译配置：

```java
this.compileConfig = CompileConfig.builder(compileConfig)
    .interruptsBefore(processedData.interruptsBefore())
    .interruptsAfter(processedData.interruptsAfter())
    .build();
```

### 7. 存储节点工厂

为了确保线程安全，存储节点动作工厂而不是实例：

```java
for (var n : processedData.nodes().elements) {
    var factory = n.actionFactory();
    Objects.requireNonNull(factory, format("action factory for node id '%s' is null!", n.id()));
    nodeFactories.put(n.id(), factory);
}
```

### 8. 处理边

评估所有边并将它们存储在映射中。这一步特别复杂，因为需要处理并行节点的情况：

```java
for (var e : processedData.edges().elements) {
    var targets = e.targets();
    if (targets.size() == 1) {
        edges.put(e.sourceId(), targets.get(0));
    } else {
        // 处理并行节点逻辑
        // ...
    }
}
```

## ProcessedNodesEdgesAndConfig类的作用

这个内部类负责处理子图的展开，这是编译过程中最复杂的部分之一：

### 处理子图展开

```java
static ProcessedNodesEdgesAndConfig process(StateGraph stateGraph, CompileConfig config)
    throws GraphStateException {
    
    var subgraphNodes = stateGraph.nodes.onlySubStateGraphNodes();

    if (subgraphNodes.isEmpty()) {
        return new ProcessedNodesEdgesAndConfig(stateGraph, config);
    }
    // 展开子图逻辑...
}
```

### 处理START节点

对子图的START节点进行特殊处理：

```java
var sgEdgeStart = processedSubGraphEdges.edgeBySourceId(START).orElseThrow();

if (sgEdgeStart.isParallel()) {
    throw new GraphStateException("subgraph not support start with parallel branches yet!");
}

var sgEdgeStartTarget = sgEdgeStart.target();
var sgEdgeStartRealTargetId = subgraphNode.formatId(sgEdgeStartTarget.id());
```

### 处理END节点

对子图的END节点进行特殊处理：

```java
var sgEdgesEnd = processedSubGraphEdges.edgesByTargetId(END);

// 处理中断（之后）子图
if (interruptsAfter.contains(subgraphNode.id())) {
    // ...
}
```

## 图验证

在编译过程中，会验证图结构的有效性：

```java
void validateGraph() throws GraphStateException {
    for (var node : nodes.elements) {
        node.validate();
    }

    var edgeStart = edges.edgeBySourceId(START).orElseThrow(Errors.missingEntryPoint::exception);
    edgeStart.validate(nodes);

    for (Edge edge : edges.elements) {
        edge.validate(nodes);
    }
}
```

验证包括：
- 所有节点必须有效
- 必须存在从START节点开始的路径
- 所有边连接的节点必须存在

## 线程安全性考虑

为了确保线程安全，CompiledGraph采取了以下措施：

1. 节点工厂存储为工厂函数而非实例
2. 使用LinkedHashMap保持顺序一致性
3. 不可变配置对象

## CompiledGraph执行使用方法

编译完成后的CompiledGraph提供了多种执行方式，以满足不同的使用场景需求。

### 1. 基本执行方法

#### invoke方法
`invoke`方法是最常用的同步执行方式，接受输入数据并返回最终的[OverAllState](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/OverAllState.java#L53-L53)：

```java
public Optional<OverAllState> invoke(Map<String, Object> inputs)
```

示例：
```java
Map<String, Object> inputs = Map.of("message", "Hello, world!");
Optional<OverAllState> result = compiledGraph.invoke(inputs);
```

也可以传入[RunnableConfig](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/RunnableConfig.java#L35-L35)配置：
```java
public Optional<OverAllState> invoke(Map<String, Object> inputs, RunnableConfig config)
```

#### invokeAndGetOutput方法
此方法返回最终的[NodeOutput](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/NodeOutput.java#L31-L31)而不是状态：
```java
public Optional<NodeOutput> invokeAndGetOutput(Map<String, Object> inputs)
```

### 2. 流式执行方法

#### stream方法
`stream`方法提供了反应式流式执行，适用于需要实时处理输出的场景：

```java
public Flux<NodeOutput> stream(Map<String, Object> inputs)
```

此方法返回[Flux<NodeOutput>](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/NodeOutput.java#L31-L31)，可以订阅并处理每个节点的输出：

```java
Flux<NodeOutput> stream = compiledGraph.stream(inputs);
stream.subscribe(
    output -> System.out.println("Received output from node: " + output.node()),
    error -> System.err.println("Error occurred: " + error),
    () -> System.out.println("Stream completed")
);
```

带配置的流式执行：
```java
public Flux<NodeOutput> stream(Map<String, Object> inputs, RunnableConfig config)
```

#### streamSnapshots方法
此方法提供快照流，可用于监控执行过程中的状态变化：
```java
public Flux<NodeOutput> streamSnapshots(Map<String, Object> inputs, RunnableConfig config)
```

### 3. 响应流方法

#### graphResponseStream方法
提供更高级别的响应流，返回[GraphResponse<NodeOutput>](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/GraphResponse.java#L29-L29)：

```java
public Flux<GraphResponse<NodeOutput>> graphResponseStream(Map<String, Object> inputs, RunnableConfig config)
```

获取单次响应：
```java
public GraphResponse<NodeOutput> invokeAndGetResponse(Map<String, Object> inputs, RunnableConfig config)
```

### 4. 配置参数详解

执行时可以通过[RunnableConfig](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/RunnableConfig.java#L35-L35)进行详细配置：

```java
RunnableConfig config = RunnableConfig.builder()
    .threadId("unique-thread-id")           // 设置线程ID，用于状态跟踪
    .checkPointId("checkpoint-id")          // 设置检查点ID，用于恢复执行
    .nextNode("specific-node-id")           // 指定下一个执行节点
    .streamMode(CompiledGraph.StreamMode.VALUES) // 设置流模式
    .build();
```

### 5. 实际应用示例

以下是使用CompiledGraph的实际示例：

```java
// 创建输入数据
Map<String, Object> inputs = Map.of(
    "message", "Hello, how are you?",
    "user_id", "12345"
);

// 创建配置
RunnableConfig config = RunnableConfig.builder()
    .threadId("session-" + UUID.randomUUID())
    .build();

// 方式1：简单调用
Optional<OverAllState> result = compiledGraph.invoke(inputs, config);

// 方式2：流式调用（适合实时处理）
Flux<NodeOutput> stream = compiledGraph.stream(inputs, config);
stream.subscribe(output -> {
    System.out.println("Node: " + output.node());
    System.out.println("State: " + output.state());
});

// 方式3：获取最后一次输出
Optional<NodeOutput> lastOutput = compiledGraph.invokeAndGetOutput(inputs, config);
```

### 6. 状态管理和持久化

CompiledGraph支持状态管理和持久化：

- **检查点机制**：通过配置的[SaverConfig](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/checkpoint/config/SaverConfig.java#L25-L25)自动保存执行状态
- **状态恢复**：使用相同[threadId](file:///C:/Users/PC/.m2/repository/com/alibaba/cloud/ai/spring-ai-alibaba-graph-core/1.1.0.0/spring-ai-alibaba-graph-core-1.1.0.0-sources.jar!/com/alibaba/cloud/ai/graph/RunnableConfig.java#L42-L42)可以恢复之前的执行
- **状态查询**：可以获取当前状态或历史状态

```java
// 获取当前状态
StateSnapshot currentState = compiledGraph.getState(config);

// 获取状态历史
Collection<StateSnapshot> history = compiledGraph.getStateHistory(config);

// 更新状态
Map<String, Object> updates = Map.of("variable", "new_value");
RunnableConfig newConfig = compiledGraph.updateState(config, updates);
```

## 总结

StateGraph到CompiledGraph的编译过程主要包括以下步骤：

1. 验证图结构的有效性
2. 处理子图展开和嵌套结构
3. 提取和合并键策略
4. 验证中断点
5. 存储节点工厂和边信息
6. 准备执行所需的所有元数据

编译后的CompiledGraph可以直接用于执行图操作，提供了线程安全的执行环境，支持状态管理、中断处理、并行执行等功能。它提供了多种执行方式，包括同步调用、流式处理和响应流，以满足不同应用场景的需求。