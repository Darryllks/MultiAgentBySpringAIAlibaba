package com.lks.simplegraph;

import com.lks.simplegraph.core.State;
import com.lks.simplegraph.edges.Edge;
import com.lks.simplegraph.nodes.Node;

import java.util.*;

/**
 * 状态图，定义图的结构和节点连接关系
 */
public class StateGraph {
    private final Map<String, Node> nodes;
    private final List<Edge> edges;
    private final String entryPoint;

    public StateGraph(String entryPoint) {
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.entryPoint = entryPoint;
    }

    /**
     * 添加节点到图中
     */
    public StateGraph addNode(String nodeId, Node node) {
        nodes.put(nodeId, node);
        return this;
    }

    /**
     * 添加边到图中
     */
    public StateGraph addEdge(String source, String target) {
        edges.add(new Edge(source, target));
        return this;
    }

    /**
     * 添加条件边到图中
     */
    public StateGraph addConditionalEdge(String source, String target, String condition) {
        edges.add(new Edge(source, target, condition));
        return this;
    }

    /**
     * 编译图结构为可执行的图
     */
    public CompiledGraph compile() {
        return new CompiledGraph(this);
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public String getEntryPoint() {
        return entryPoint;
    }
}