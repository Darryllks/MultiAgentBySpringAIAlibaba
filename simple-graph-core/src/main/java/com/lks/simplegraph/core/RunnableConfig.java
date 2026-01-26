package com.lks.simplegraph.core;

import java.util.Map;
import java.util.HashMap;

/**
 * 可运行配置，包含执行图时的配置参数
 */
public class RunnableConfig {
    private String threadId;  // 用于标识不同的执行线程
    private String checkpointId;  // 检查点ID，用于从特定位置恢复执行
    private Map<String, Object> configurable;  // 可配置参数
    private Map<String, Object> metadata;  // 元数据信息
    
    public RunnableConfig() {
        this.configurable = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public RunnableConfig(String threadId) {
        this.threadId = threadId;
        this.configurable = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
    
    public String getCheckpointId() {
        return checkpointId;
    }
    
    public void setCheckpointId(String checkpointId) {
        this.checkpointId = checkpointId;
    }
    
    public Map<String, Object> getConfigurable() {
        return configurable;
    }
    
    public void setConfigurable(Map<String, Object> configurable) {
        this.configurable = configurable;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void putConfig(String key, Object value) {
        this.configurable.put(key, value);
    }
    
    public Object getConfig(String key) {
        return this.configurable.get(key);
    }
    
    public void putMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
}