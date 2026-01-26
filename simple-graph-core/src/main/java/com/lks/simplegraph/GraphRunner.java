package com.lks.simplegraph;

import com.lks.simplegraph.core.State;
import com.lks.simplegraph.core.RunnableConfig;
import com.lks.simplegraph.core.impl.SimpleState;
import com.lks.simplegraph.executor.MainGraphExecutor;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于Project Reactor的响应式图执行引擎。这个类经过重构，
 * 使用面向对象原则（继承、多态、封装）来实现更好的关注点分离和可读性。
 */
public class GraphRunner {

    private final CompiledGraph compiledGraph;

    private final RunnableConfig config;

    private final AtomicReference<Object> resultValue = new AtomicReference<>();

    // 主执行流处理器 - 体现了封装特性
    private final MainGraphExecutor mainGraphExecutor;

    public GraphRunner(CompiledGraph compiledGraph, RunnableConfig config) {
        this.compiledGraph = compiledGraph;
        this.config = config;
        // 初始化主执行处理器 - 体现了封装特性
        this.mainGraphExecutor = new MainGraphExecutor();
    }

    public Flux<GraphResponse<NodeOutput>> run(State initialState) {
        return Flux.defer(() -> {
            try {
                GraphRunnerContext context = new GraphRunnerContext(initialState, config, compiledGraph);
                // 委托给主执行处理器 - 体现了多态特性
                return mainGraphExecutor.execute(context, resultValue);
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }

    public Optional<Object> resultValue() {
        return Optional.ofNullable(resultValue.get());
    }

}