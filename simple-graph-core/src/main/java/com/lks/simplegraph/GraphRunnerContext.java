package com.lks.simplegraph;

import com.lks.simplegraph.action.Command;
import com.lks.simplegraph.core.State;
import com.lks.simplegraph.core.RunnableConfig;
import com.lks.simplegraph.core.impl.SimpleState;
import com.lks.simplegraph.nodes.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 图执行器上下文
 */
public class GraphRunnerContext {

    public static final String INTERRUPT_AFTER = "__interrupt_after__";

    private State overallState;
    private final RunnableConfig config;
    private final CompiledGraph compiledGraph;
    private String currentNodeId;
    private String nextNodeId;
    private final Map<String, Consumer<Object>> listeners = new ConcurrentHashMap<>();

    public GraphRunnerContext(State initialState, RunnableConfig config, CompiledGraph compiledGraph) {
        this.overallState = initialState;
        this.config = config;
        this.compiledGraph = compiledGraph;
    }

    public State getOverallState() {
        return overallState;
    }

    public RunnableConfig getConfig() {
        return config;
    }

    public CompiledGraph getCompiledGraph() {
        return compiledGraph;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public State cloneState(State original) {
        // 创建新的SimpleState实例，复制原始状态的数据
        return new SimpleState(original.getAll());
    }

    public void mergeIntoCurrentState(Map<String, Object> update) {
        if (update != null) {
            for (Map.Entry<String, Object> entry : update.entrySet()) {
                overallState.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public boolean shouldStop() {
        return nextNodeId == null || "__END__".equals(nextNodeId);
    }

    public boolean isMaxIterationsReached() {
        // 这里可以添加最大迭代次数检查逻辑
        return false;
    }

    public boolean isStartNode() {
        return currentNodeId == null;
    }

    public boolean isEndNode() {
        return "__END__".equals(currentNodeId);
    }

    public boolean shouldInterrupt() {
        // 简单的中断检查逻辑
        return false;
    }

    public Node getNodeAction(String nodeId) {
        // 通过compiledGraph的getNodes()方法获取节点
        return compiledGraph.getNodes().get(nodeId);
    }

    public Command getEntryPoint() {
        return new Command(compiledGraph.getEntryPoint());
    }

    public Command nextNodeId(String currentNodeId, Object stateData) {
        // 根据当前节点和状态确定下一个节点
        List<String> nextNodes = compiledGraph.getAdjacencyList().get(currentNodeId);
        if (nextNodes != null && !nextNodes.isEmpty()) {
            // 简单的路由逻辑 - 选择第一个可用节点
            String nextNodeId = getNextNode(currentNodeId, overallState);
            return new Command(nextNodeId);
        }
        return new Command("__END__");
    }

    private String getNextNode(String currentNodeId, State state) {
        List<String> nextNodes = compiledGraph.getAdjacencyList().get(currentNodeId);
        if (nextNodes == null || nextNodes.isEmpty()) {
            return "__END__";
        }

        // 如果只有一个后续节点，直接返回
        if (nextNodes.size() == 1) {
            return nextNodes.get(0);
        }

        // 如果有多个后续节点，根据条件判断
        for (var edge : compiledGraph.getEdges()) {
            if (edge.getSource().equals(currentNodeId)) {
                if (edge.hasCondition()) {
                    // 这里可以实现条件判断逻辑
                    if (evaluateCondition(edge.getCondition(), state)) {
                        return edge.getTarget();
                    }
                } else {
                    // 如果没有条件，返回第一个无条件节点
                    return edge.getTarget();
                }
            }
        }

        // 默认返回第一个节点
        return nextNodes.get(0);
    }

    private boolean evaluateCondition(String condition, State state) {
        // 简化实现 - 实际应用中应有更复杂的条件评估
        Object value = state.get(condition);
        return value != null && Boolean.TRUE.equals(value);
    }

    public NodeOutput buildNodeOutput(String nodeId) {
        return new NodeOutput(overallState.getAll(), nodeId);
    }

    public NodeOutput buildNodeOutputAndAddCheckpoint(Map<String, Object> updateState) {
        mergeIntoCurrentState(updateState);
        NodeOutput output = new NodeOutput(overallState.getAll(), currentNodeId);
        return output;
    }

    public NodeOutput buildOutput(String nodeId, Optional<Object> checkpoint) {
        return new NodeOutput(overallState.getAll(), nodeId);
    }

    public void doListeners(String event, Object data) {
        // 简单的监听器调用
        if (listeners.containsKey(event)) {
            listeners.get(event).accept(data);
        }
    }

    public void addListener(String event, Consumer<Object> listener) {
        listeners.put(event, listener);
    }

    // 添加一些辅助方法
    public Object getCurrentStateData() {
        return overallState.getAll();
    }

    public Optional<Object> getReturnFromEmbedAndReset() {
        // 模拟从嵌入式执行中返回的值
        return Optional.empty();
    }

    public void setReturnFromEmbedWithValue(Object value) {
        // 设置嵌入式执行返回的值
    }

    public Optional<String> getResumeFromAndReset() {
        // 获取恢复执行的节点ID
        return Optional.empty();
    }

    public NodeOutput buildStreamingOutput(Object element, String nodeId, boolean isStreaming) {
        // 构建流式输出
        return new NodeOutput(Collections.singletonMap("data", element), nodeId);
    }

    public NodeOutput buildStreamingOutput(Object element, Object response, String nodeId, boolean isStreaming) {
        // 构建流式输出（带响应对象）
        Map<String, Object> data = new HashMap<>();
        data.put("element", element);
        data.put("response", response);
        return new NodeOutput(data, nodeId);
    }

    public Optional<Object> addCheckpoint(String nodeId, String nextNodeId) {
        // 添加检查点
        return Optional.empty();
    }
}