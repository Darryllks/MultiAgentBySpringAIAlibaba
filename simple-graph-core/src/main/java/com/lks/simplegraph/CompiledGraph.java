package com.lks.simplegraph;

import com.lks.simplegraph.core.Checkpoint;
import com.lks.simplegraph.core.State;
import com.lks.simplegraph.core.RunnableConfig;
import com.lks.simplegraph.core.impl.SimpleState;
import com.lks.simplegraph.nodes.Node;
import com.lks.simplegraph.edges.Edge;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * 编译后的图，可直接执行
 */
public class CompiledGraph {
    private final StateGraph stateGraph;
    private final Map<String, List<String>> adjacencyList;
    
    // 检查点存储
    private final Map<String, List<Checkpoint>> checkpoints;

    public CompiledGraph(StateGraph stateGraph) {
        this.stateGraph = stateGraph;
        this.adjacencyList = buildAdjacencyList();
        this.checkpoints = new HashMap<>();
    }

    /**
     * 构建邻接表以快速查找节点连接关系
     */
    private Map<String, List<String>> buildAdjacencyList() {
        Map<String, List<String>> adj = new HashMap<>();
        
        for (var edge : stateGraph.getEdges()) {
            adj.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
        }
        
        return adj;
    }

    /**
     * 执行图
     */
    public State invoke(Map<String, Object> inputs) {
        return invoke(inputs, new RunnableConfig());
    }
    
    /**
     * 执行图，支持配置参数
     */
    public State invoke(Map<String, Object> inputs, RunnableConfig config) {
        // 使用GraphRunner执行图
        GraphRunner runner = new GraphRunner(this, config);
        
        // 使用Reactor的阻塞方式等待执行完成
        reactor.core.publisher.Flux<GraphResponse<NodeOutput>> flux = runner.run(new SimpleState(inputs));
        
        // 收集最终结果
        java.util.List<GraphResponse<NodeOutput>> results = flux.collectList().block();
        
        if (results != null && !results.isEmpty()) {
            GraphResponse<NodeOutput> lastResult = results.get(results.size() - 1);
            if (lastResult.resultValue().isPresent()) {
                Object result = lastResult.resultValue().get();
                if (result instanceof State) {
                    return (State) result;
                } else if (result instanceof java.util.Map) {
                    return new SimpleState((java.util.Map<String, Object>) result);
                }
            }
        }
        
        // 如果无法获取结果，返回初始状态
        return new SimpleState(inputs);
    }

    /**
     * 确定下一个节点
     */
    private String getNextNode(String currentNodeId, State state) {
        List<String> nextNodes = adjacencyList.get(currentNodeId);
        if (nextNodes == null || nextNodes.isEmpty()) {
            return null;
        }

        // 如果只有一个后续节点，直接返回
        if (nextNodes.size() == 1) {
            return nextNodes.get(0);
        }

        // 如果有多个后续节点，根据条件判断
        for (var edge : stateGraph.getEdges()) {
            if (edge.getSource().equals(currentNodeId)) {
                if (edge.hasCondition()) {
                    // 这里可以实现条件判断逻辑
                    // 简化实现：如果条件匹配则返回对应节点
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

    /**
     * 简单的条件评估实现
     */
    private boolean evaluateCondition(String condition, State state) {
        // 简化实现 - 实际应用中应有更复杂的条件评估
        // 这里我们假设条件是类似 "has_tools" 或 "need_to_continue" 的键名
        // 如果状态中有这个键且值为true，则条件成立
        Object value = state.get(condition);
        return value != null && Boolean.TRUE.equals(value);
    }
    
    /**
     * 创建检查点
     */
    private void createCheckpoint(String nodeId, String nextNodeId, State state, RunnableConfig config) {
        String threadId = config.getThreadId() != null ? config.getThreadId() : "default";
        String checkpointId = threadId + "_" + nodeId + "_" + System.currentTimeMillis();
        
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setId(checkpointId);
        checkpoint.setThreadId(threadId);
        checkpoint.setNodeId(nodeId);
        checkpoint.setNextNodeId(nextNodeId);
        checkpoint.setState(cloneState(state));
        checkpoint.setConfig(config);
        
        checkpoints.computeIfAbsent(threadId, k -> new ArrayList<>()).add(checkpoint);
        
        System.out.println("创建检查点: " + checkpointId + " 节点: " + nodeId + " 下一节点: " + nextNodeId);
    }
    
    /**
     * 获取指定ID的检查点
     */
    public Checkpoint getCheckpoint(String checkpointId, String threadId) {
        if (threadId == null) {
            threadId = "default";
        }
        
        List<Checkpoint> threadCheckpoints = checkpoints.get(threadId);
        if (threadCheckpoints != null) {
            for (Checkpoint cp : threadCheckpoints) {
                if (checkpointId.equals(cp.getId())) {
                    return cp;
                }
            }
        }
        return null;
    }
    
    /**
     * 获取特定线程的所有检查点
     */
    public List<Checkpoint> getCheckpoints(String threadId) {
        if (threadId == null) {
            threadId = "default";
        }
        return checkpoints.getOrDefault(threadId, new ArrayList<>());
    }
    
    /**
     * 克隆状态对象
     */
    private State cloneState(State original) {
        // 创建新的SimpleState实例，复制原始状态的数据
        return new SimpleState(original.getAll());
    }
    
    // 添加getter方法以支持GraphRunner
    public Map<String, List<String>> getAdjacencyList() {
        return adjacencyList;
    }
    
    public List<Edge> getEdges() {
        return stateGraph.getEdges();
    }
    
    public Map<String, Node> getNodes() {
        return stateGraph.getNodes();
    }
    
    public String getEntryPoint() {
        return stateGraph.getEntryPoint();
    }
    
    public StateGraph getStateGraph() {
        return stateGraph;
    }
}