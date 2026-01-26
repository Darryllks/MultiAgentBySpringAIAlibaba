package com.lks.simplegraph;

import java.util.Map;

/**
 * 节点输出包装类
 */
public class NodeOutput {

    private Map<String, Object> data;
    private String nodeId;
    private boolean subGraph = false;
    private boolean completedExceptionally = false;

    public NodeOutput(Map<String, Object> data, String nodeId) {
        this.data = data;
        this.nodeId = nodeId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isSubGraph() {
        return subGraph;
    }

    public void setSubGraph(boolean subGraph) {
        this.subGraph = subGraph;
    }

    public boolean isCompletedExceptionally() {
        return completedExceptionally;
    }

    public void setCompletedExceptionally(boolean completedExceptionally) {
        this.completedExceptionally = completedExceptionally;
    }

    public NodeOutput join() {
        return this;
    }
}