package com.lks.simplegraph.executor;

import com.lks.simplegraph.GraphRunnerContext;
import com.lks.simplegraph.GraphResponse;
import com.lks.simplegraph.NodeOutput;

import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 图执行处理器的基类。此类通过继承演示了面向对象的设计，
 * 提供所有执行器共享的通用功能。
 */
public abstract class BaseGraphExecutor {

    /**
     * 子类必须实现的抽象方法。这体现了多态性，
     * 因为每个子类将提供自己的实现。
     * @param context 图执行器上下文
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含执行结果的GraphResponse流
     */
    public abstract Flux<GraphResponse<NodeOutput>> execute(GraphRunnerContext context,
            AtomicReference<Object> resultValue);

    /**
     * 可由子类使用的受保护方法。这通过封装演示了对公共
     * 功能的受控访问。
     * @param context 图执行器上下文
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含完成处理结果的GraphResponse流
     */
    @SuppressWarnings("unchecked")
    protected Flux<GraphResponse<NodeOutput>> handleCompletion(GraphRunnerContext context,
            AtomicReference<Object> resultValue) {
        return Flux.defer(() -> {
            try {
                Object stateData = context.getOverallState().getAll();
                resultValue.set(stateData);
                
                // 创建一个NodeOutput包装结果
                NodeOutput output = new NodeOutput((HashMap<String, Object>) stateData, "__END__");
                return Flux.just(GraphResponse.done(output));
            } catch (Exception e) {
                return Flux.just(GraphResponse.error(e));
            }
        });
    }
}