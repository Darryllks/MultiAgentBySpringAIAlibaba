package com.lks.simplegraph.core;

import java.util.Map;

/**
 * 表示图执行过程中的状态
 */
public interface State {
    /**
     * 获取状态中的值
     */
    <T> T get(String key);

    /**
     * 设置状态中的值
     */
    <T> void set(String key, T value);

    /**
     * 获取完整状态映射
     */
    Map<String, Object> getAll();

    /**
     * 更新状态（合并操作）
     */
    void update(Map<String, Object> newState);
}