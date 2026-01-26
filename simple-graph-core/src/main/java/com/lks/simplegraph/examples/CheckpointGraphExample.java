package com.lks.simplegraph.examples;

import com.lks.simplegraph.CompiledGraph;
import com.lks.simplegraph.StateGraph;
import com.lks.simplegraph.core.State;
import com.lks.simplegraph.core.RunnableConfig;
import com.lks.simplegraph.core.Checkpoint;
import com.lks.simplegraph.core.impl.SimpleState;
import com.lks.simplegraph.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示检查点功能的图示例
 */
public class CheckpointGraphExample {
    
    public static void main(String[] args) {
        demonstrateCheckpointFunctionality();
    }

    /**
     * 演示检查点功能
     */
    public static void demonstrateCheckpointFunctionality() {
        System.out.println("=== 检查点功能演示 ===");
        
        // 创建一个图，模拟多步骤处理流程
        StateGraph graph = new StateGraph("step1");
        
        // 添加处理步骤节点
        graph.addNode("step1", new Step1Node())
             .addNode("step2", new Step2Node())
             .addNode("step3", new Step3Node())
             .addNode("final", new FinalNode());
        
        // 添加边
        graph.addEdge("step1", "step2")
             .addEdge("step2", "step3")
             .addEdge("step3", "final");
        
        // 编译图
        CompiledGraph compiledGraph = graph.compile();
        
        // 准备输入
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("initial_value", "Starting data");
        
        // 创建配置，指定线程ID
        RunnableConfig config = new RunnableConfig("thread-001");
        
        System.out.println("--- 第一次执行 ---");
        // 执行图
        State result = compiledGraph.invoke(inputs, config);
        System.out.println("第一次执行完成，结果: " + result.get("final_result"));
        
        // 显示所有检查点
        System.out.println("\n--- 检查点列表 ---");
        List<Checkpoint> checkpoints = compiledGraph.getCheckpoints("thread-001");
        for (int i = 0; i < checkpoints.size(); i++) {
            Checkpoint cp = checkpoints.get(i);
            System.out.println((i+1) + ". 检查点ID: " + cp.getId() + ", 节点: " + cp.getNodeId() + ", 时间: " + cp.getTimestamp());
        }
        
        System.out.println("\n--- 从第2个检查点恢复执行 ---");
        // 从第二个检查点恢复执行
        if (checkpoints.size() > 1) {
            Checkpoint restorePoint = checkpoints.get(1); // 从step2的检查点恢复
            
            RunnableConfig restoreConfig = new RunnableConfig("thread-002");
            restoreConfig.setCheckpointId(restorePoint.getId());
            
            System.out.println("从检查点恢复: " + restorePoint.getId() + " 节点: " + restorePoint.getNodeId());
            
            // 从检查点恢复执行
            State restoredResult = compiledGraph.invoke(inputs, restoreConfig);
            System.out.println("从检查点恢复执行完成，结果: " + restoredResult.get("final_result"));
        }
    }

    // 步骤1节点
    static class Step1Node implements Node {
        @Override
        public State execute(State state, RunnableConfig config) {
            System.out.println("[Step1] 执行步骤1处理...");
            String initialValue = state.get("initial_value");
            state.set("step1_result", "Step1 processed: " + initialValue);
            state.set("current_step", "step1");
            System.out.println("[Step1] 配置信息: ThreadId = " + config.getThreadId());
            return state;
        }
    }

    // 步骤2节点
    static class Step2Node implements Node {
        @Override
        public State execute(State state, RunnableConfig config) {
            System.out.println("[Step2] 执行步骤2处理...");
            String step1Result = state.get("step1_result");
            state.set("step2_result", "Step2 processed: " + step1Result);
            state.set("current_step", "step2");
            System.out.println("[Step2] 配置信息: ThreadId = " + config.getThreadId());
            return state;
        }
    }

    // 步骤3节点
    static class Step3Node implements Node {
        @Override
        public State execute(State state, RunnableConfig config) {
            System.out.println("[Step3] 执行步骤3处理...");
            String step2Result = state.get("step2_result");
            state.set("step3_result", "Step3 processed: " + step2Result);
            state.set("current_step", "step3");
            System.out.println("[Step3] 配置信息: ThreadId = " + config.getThreadId());
            return state;
        }
    }

    // 最终节点
    static class FinalNode implements Node {
        @Override
        public State execute(State state, RunnableConfig config) {
            System.out.println("[Final] 执行最终处理...");
            String step3Result = state.get("step3_result");
            state.set("final_result", "Final result: " + step3Result);
            state.set("current_step", "final");
            System.out.println("[Final] 配置信息: ThreadId = " + config.getThreadId());
            return state;
        }
    }
}