package com.lks.simplegraph.core;

import com.lks.simplegraph.core.impl.SimpleState;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 检查点，用于保存图执行过程中的状态快照
 */
public class Checkpoint {
    private String id;
    private String threadId;
    private String nodeId;  // 当前节点ID
    private String nextNodeId;  // 下一个节点ID（可选，用于精确恢复路径）
    private State state;    // 当前状态
    private RunnableConfig config;  // 执行配置
    private LocalDateTime timestamp;  // 创建时间戳
    private int sequenceNumber;  // 序列号，用于排序
    
    public Checkpoint() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Checkpoint(String id, String threadId, String nodeId, State state, RunnableConfig config) {
        this.id = id;
        this.threadId = threadId;
        this.nodeId = nodeId;
        this.state = state;
        this.config = config;
        this.timestamp = LocalDateTime.now();
    }
    
    public Checkpoint(String id, String threadId, String nodeId, String nextNodeId, State state, RunnableConfig config, int sequenceNumber) {
        this.id = id;
        this.threadId = threadId;
        this.nodeId = nodeId;
        this.nextNodeId = nextNodeId;
        this.state = state;
        this.config = config;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getNextNodeId() { return nextNodeId; }
    public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
    
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    
    public RunnableConfig getConfig() { return config; }
    public void setConfig(RunnableConfig config) { this.config = config; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    
    /**
     * 将检查点转换为Map格式
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("threadId", this.threadId);
        map.put("nodeId", this.nodeId);
        map.put("nextNodeId", this.nextNodeId);
        map.put("state", this.state.getAll());
        map.put("timestamp", this.timestamp.toString());
        map.put("sequenceNumber", this.sequenceNumber);
        return map;
    }
    
    /**
     * 从Map创建检查点
     */
    public static Checkpoint fromMap(Map<String, Object> map) {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setId((String) map.get("id"));
        checkpoint.setThreadId((String) map.get("threadId"));
        checkpoint.setNodeId((String) map.get("nodeId"));
        checkpoint.setNextNodeId((String) map.get("nextNodeId"));
        checkpoint.setTimestamp(LocalDateTime.parse((String) map.get("timestamp")));
        checkpoint.setSequenceNumber((Integer) map.get("sequenceNumber"));
        
        // 从map中获取状态数据并重建State对象
        @SuppressWarnings("unchecked")
        Map<String, Object> stateData = (Map<String, Object>) map.get("state");
        if (stateData != null) {
            checkpoint.setState(new SimpleState(stateData));
        }
        
        return checkpoint;
    }
}