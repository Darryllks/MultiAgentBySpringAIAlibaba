package com.lks.simplegraph.executor;

import com.lks.simplegraph.CompiledGraph;
import com.lks.simplegraph.GraphResponse;
import com.lks.simplegraph.GraphRunnerContext;
import com.lks.simplegraph.NodeOutput;
import com.lks.simplegraph.action.Command;
import com.lks.simplegraph.edges.Edge;
import com.lks.simplegraph.nodes.Node;
import com.lks.simplegraph.StateGraph;
import com.lks.simplegraph.executor.MainGraphExecutor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

/**
 * 节点执行器，处理节点执行和结果处理。此类通过继承BaseGraphExecutor演示了
 * 继承。它还通过其特定的execute实现演示了多态性。
 */
public class NodeExecutor extends BaseGraphExecutor {

    private final MainGraphExecutor mainGraphExecutor;

    public NodeExecutor(MainGraphExecutor mainGraphExecutor) {
        this.mainGraphExecutor = mainGraphExecutor;
    }

    /**
     * execute方法的具体实现。这演示了多态性，因为它提供了
     * 节点执行的特定实现。
     * @param context 图执行器上下文
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含执行结果的GraphResponse流
     */
    @Override
    public Flux<GraphResponse<NodeOutput>> execute(GraphRunnerContext context, AtomicReference<Object> resultValue) {
        return executeNode(context, resultValue);
    }

    /**
     * 执行节点并处理其结果。
     * @param context 图执行器上下文
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含节点执行结果的GraphResponse流
     */
    private Flux<GraphResponse<NodeOutput>> executeNode(GraphRunnerContext context,
            AtomicReference<Object> resultValue) {
        try {
            context.setCurrentNodeId(context.getNextNodeId());
            String currentNodeId = context.getCurrentNodeId();

            if (!context.getCompiledGraph().getNodes().containsKey(currentNodeId)) {
                return Flux.just(GraphResponse.error(new RuntimeException("Node not found: " + currentNodeId)));
            }

            var action = context.getNodeAction(currentNodeId);

            if (action == null) {
                return Flux.just(GraphResponse.error(new RuntimeException("Missing node: " + currentNodeId)));
            }

            // 执行节点并获取结果
            var updatedState = action.execute(context.getOverallState(), context.getConfig());

            // 处理执行结果
            return handleActionResult(context, updatedState.getAll(), resultValue);
        } catch (Exception e) {
            return Flux.just(GraphResponse.error(e));
        }
    }

    /**
     * 处理动作结果并返回适当响应。
     * @param context 图执行器上下文
     * @param updateState 从动作更新的状态
     * @param resultValue 用于存储结果值的原子引用
     * @return 包含动作结果处理的GraphResponse流
     */
    private Flux<GraphResponse<NodeOutput>> handleActionResult(GraphRunnerContext context,
            Map<String, Object> updateState, AtomicReference<Object> resultValue) {
        try {
            context.mergeIntoCurrentState(updateState);

            Command nextCommand = context.nextNodeId(context.getCurrentNodeId(), context.getOverallState());
            context.setNextNodeId(nextCommand.gotoNode());
            NodeOutput output = context.buildNodeOutputAndAddCheckpoint(updateState);

            // 递归调用主执行处理器
            return Flux.just(GraphResponse.of(output))
                .concatWith(Flux.defer(() -> mainGraphExecutor.execute(context, resultValue)));
        } catch (Exception e) {
            return Flux.just(GraphResponse.error(e));
        }
    }
}