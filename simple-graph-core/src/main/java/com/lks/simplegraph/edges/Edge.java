package com.lks.simplegraph.edges;

/**
 * 图中的边，定义节点之间的连接关系
 */
public class Edge {
    private final String source;  // 源节点
    private final String target;  // 目标节点
    private final String condition; // 条件（可选）

    public Edge(String source, String target) {
        this(source, target, null);
    }

    public Edge(String source, String target, String condition) {
        this.source = source;
        this.target = target;
        this.condition = condition;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getCondition() {
        return condition;
    }

    public boolean hasCondition() {
        return condition != null;
    }
}