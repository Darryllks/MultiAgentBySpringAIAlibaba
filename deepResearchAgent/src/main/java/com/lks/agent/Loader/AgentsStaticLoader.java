package com.lks.agent.Loader;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.lks.agent.Agents.DeepResearchAgent;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Configuration
public class AgentsStaticLoader implements AgentLoader {

    private Map<String, Agent> agents = new ConcurrentHashMap<>();

    public AgentsStaticLoader(ToolCallbackProvider toolCallbackProvider) {

        // 获取MCP客户端提供的工具回调列表
        List<ToolCallback> toolCallbacks = Arrays.asList(toolCallbackProvider.getToolCallbacks());

        // 输出加载的工具数量信息
        System.out.println("Loaded MCP tool callbacks: " + toolCallbacks.size());

        // 创建DeepResearch研究代理实例
        ReactAgent researchAgent = new DeepResearchAgent().getResearchAgent(toolCallbacks);

        // 生成代理图的PlantUML表示（用于可视化调试）
        GraphRepresentation representation = researchAgent.getAndCompileGraph().stateGraph.getGraph(GraphRepresentation.Type.PLANTUML);

        // 输出图形表示内容
        System.out.println(representation.content());

        this.agents.put("research_agent", researchAgent);
    }

    @NotNull
    @Override
    public List<String> listAgents() {
        return agents.keySet().stream().toList();
    }

    @Override
    public Agent loadAgent(String name) {
        // 参数验证：检查代理名称是否有效
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent name cannot be null or empty");
        }

        // 从映射表中查找代理实例
        Agent agent = agents.get(name);
        if (agent == null) {
            throw new NoSuchElementException("Agent not found: " + name);
        }

        return agent;
    }
}
