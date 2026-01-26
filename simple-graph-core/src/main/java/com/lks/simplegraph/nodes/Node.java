package com.lks.simplegraph.nodes;

import com.lks.simplegraph.core.State;
import com.lks.simplegraph.core.RunnableConfig;

/**
 * 图中的节点，代表一个处理单元
 */
@FunctionalInterface
public interface Node {
    /**
     * 执行节点逻辑并返回更新后的状态
     *
     * @param state 当前状态
     * @param config 执行配置
     * @return 处理后的状态
     */
    State execute(State state, RunnableConfig config);
    
    /**
     * 默认execute方法，兼容旧版本
     *
     * @param state 当前状态
     * @return 处理后的状态
     */
    default State execute(State state) {
        return execute(state, new RunnableConfig());
    }
}