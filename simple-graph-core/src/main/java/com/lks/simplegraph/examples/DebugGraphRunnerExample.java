package com.lks.simplegraph.examples;

import com.lks.simplegraph.CompiledGraph;
import com.lks.simplegraph.GraphResponse;
import com.lks.simplegraph.GraphRunner;
import com.lks.simplegraph.NodeOutput;
import com.lks.simplegraph.StateGraph;
import com.lks.simplegraph.core.RunnableConfig;
import com.lks.simplegraph.core.State;
import com.lks.simplegraph.core.impl.SimpleState;
import com.lks.simplegraph.nodes.Node;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * GraphRunner调试示例
 */
public class DebugGraphRunnerExample {

    public static void main(String[] args) {
        // 创建一个简单的状态图
        StateGraph graph = new StateGraph("start");

        // 定义节点
        Node startNode = (state, config) -> {
            System.out.println("执行开始节点");
            state.set("message", "Hello from start node");
            state.set("step", 1);
            return state;
        };

        Node endNode = (state, config) -> {
            System.out.println("执行结束节点");
            String message = state.get("message");
            state.set("message", message + " -> processed by end node");
            state.set("step", 2);
            return state;
        };

        // 添加节点到图
        graph.addNode("start", startNode)
             .addNode("end", endNode)
             .addEdge("start", "end");

        // 编译图
        CompiledGraph compiledGraph = graph.compile();

        // 创建初始状态
        State initialState = new SimpleState(Map.of("initial_value", "test"));

        // 直接使用CompiledGraph的invoke方法测试
        System.out.println("直接使用CompiledGraph.invoke方法:");
        State result = compiledGraph.invoke(initialState.getAll());
        System.out.println("结果: " + result.getAll());

        System.out.println("\n使用GraphRunner:");
        // 使用GraphRunner执行图
        GraphRunner runner = new GraphRunner(compiledGraph, new RunnableConfig());

        // 执行图并处理结果
        Flux<GraphResponse<NodeOutput>> resultFlux = runner.run(initialState);

        System.out.println("开始执行图...");
        resultFlux.subscribe(
            response -> {
                System.out.println("收到响应: " + response.getStatus());
                if (response.getError().isPresent()) {
                    System.out.println("错误: " + response.getError().get().getMessage());
                    response.getError().get().printStackTrace();
                }
                if (response.getOutput() != null) {
                    System.out.println("输出数据: " + response.getOutput().getData());
                }
            },
            error -> {
                System.err.println("执行出错: " + error.getMessage());
                error.printStackTrace();
            },
            () -> {
                System.out.println("图执行完成！");
                // 输出最终结果
                runner.resultValue().ifPresent(result1 -> {
                    System.out.println("最终结果: " + result1);
                });
            }
        );

        // 等待执行完成
        try {
            Thread.sleep(2000); // 等待2秒让异步执行完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}