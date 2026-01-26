package com.lks.simplegraph.executor;

import com.lks.simplegraph.GraphRunnerContext;
import com.lks.simplegraph.GraphResponse;
import com.lks.simplegraph.NodeOutput;
import com.lks.simplegraph.action.Command;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 主图执行器，处理主要执行流程。此类通过继承BaseGraphExecutor演示了
 * 继承。它还通过其特定的execute实现演示了多态性。
 */
public class MainGraphExecutor extends BaseGraphExecutor {

    private final NodeExecutor nodeExecutor;

    public MainGraphExecutor() {
        this.nodeExecutor = new NodeExecutor(this);
    }

    /**
     * execute方法的具体实现。这演示了多态性，因为它提供了
     * 主执行流程的特定实现。
     * @param context 图执行器上下文
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含执行结果的GraphResponse流
     */
    @Override
    public Flux<GraphResponse<NodeOutput>> execute(GraphRunnerContext context, AtomicReference<Object> resultValue) {
        try {
            if (context.shouldStop() || context.isMaxIterationsReached()) {
                return handleCompletion(context, resultValue);
            }

            if (context.isStartNode()) {
                return handleStartNode(context);
            }

            if (context.isEndNode()) {
                return handleEndNode(context, resultValue);
            }

            if (context.shouldInterrupt()) {
                // 简单的中断处理
                return Flux.just(GraphResponse.done(context.buildNodeOutput(context.getCurrentNodeId())));
            }

            return nodeExecutor.execute(context, resultValue);
        } catch (Exception e) {
            return Flux.just(GraphResponse.error(e));
        }
    }

    /**
     * 处理起始节点执行。
     * @param context 图执行器上下文
     * @return 包含起始节点处理结果的GraphResponse流
     */
    private Flux<GraphResponse<NodeOutput>> handleStartNode(GraphRunnerContext context) {
        try {
            Command nextCommand = context.getEntryPoint();
            context.setNextNodeId(nextCommand.gotoNode());

            NodeOutput output = context.buildOutput(context.getNextNodeId(), Optional.empty());

            context.setCurrentNodeId(context.getNextNodeId());
            // 递归调用主执行处理器
            return Flux.just(GraphResponse.of(output))
                .concatWith(Flux.defer(() -> execute(context, new AtomicReference<>())));
        } catch (Exception e) {
            return Flux.just(GraphResponse.error(e));
        }
    }

    /**
     * 处理结束节点执行。
     * @param context 图执行器上下文
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含结束节点处理结果的GraphResponse流
     */
    private Flux<GraphResponse<NodeOutput>> handleEndNode(GraphRunnerContext context,
            AtomicReference<Object> resultValue) {
        try {
            NodeOutput output = context.buildNodeOutput("__END__");
            return Flux.just(GraphResponse.of(output))
                .concatWith(Flux.defer(() -> handleCompletion(context, resultValue)));
        } catch (Exception e) {
            return Flux.just(GraphResponse.error(e));
        }
    }

}