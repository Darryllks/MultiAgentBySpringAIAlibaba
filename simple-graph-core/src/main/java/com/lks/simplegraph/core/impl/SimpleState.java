package com.lks.simplegraph.core.impl;

import com.lks.simplegraph.core.State;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的状态实现
 */
public class SimpleState implements State {
    private final Map<String, Object> state;

    public SimpleState() {
        this.state = new HashMap<>();
    }

    public SimpleState(Map<String, Object> initialState) {
        this.state = new HashMap<>(initialState);
    }

    @Override
    public <T> T get(String key) {
        return (T) state.get(key);
    }

    @Override
    public <T> void set(String key, T value) {
        state.put(key, value);
    }

    @Override
    public Map<String, Object> getAll() {
        return new HashMap<>(state);
    }

    @Override
    public void update(Map<String, Object> newState) {
        state.putAll(newState);
    }
}