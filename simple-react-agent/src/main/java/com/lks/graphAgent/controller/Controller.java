package com.lks.graphAgent.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
public class Controller {

    @Autowired
    @Qualifier("simplePoemReactAgent")
    private ReactAgent reactAgent;

    @GetMapping("/poem")
    public Flux<String> getPoem(@RequestParam(value = "msg", defaultValue = "做一首关于春天的诗") String msg,
                                @RequestParam(value = "thread_Id") String thread_Id) throws GraphRunnerException {
        // 创建带唯一threadId的配置，以便在Redis中跟踪状态
        RunnableConfig config = RunnableConfig.builder()
                .threadId("poem_thread_" + thread_Id)
                .build();
                
        Flux<NodeOutput> stream = reactAgent.stream(msg, config);
        
        return stream.filter(output -> {
            // 只处理包含实际内容的输出
            if (output instanceof StreamingOutput streamingOutput) {
                return streamingOutput.getOutputType() == OutputType.AGENT_MODEL_STREAMING || 
                       streamingOutput.getOutputType() == OutputType.AGENT_MODEL_FINISHED;
            }
            // 对于非StreamingOutput，检查是否是结束节点且包含有用信息
            return "__END__".equals(output.node()) && output.state() != null;
        }).mapNotNull(output -> {
            if (output instanceof StreamingOutput streamingOutput) {
                // 处理流式输出
                if (streamingOutput.getOutputType() == OutputType.AGENT_MODEL_STREAMING) {
                    Message message = streamingOutput.message();
                    if(message instanceof AssistantMessage assistantMessage){
                        // 尝试获取各种可能的思维过程字段
                        Object reasoningContent = assistantMessage.getMetadata().get("reasoningContent");
                        
                        // 打印所有可用的元数据键，帮助调试
                        System.out.println("Available metadata keys: " + assistantMessage.getMetadata().keySet());
                        
                        if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                            System.out.println("思考中。。。" + reasoningContent);
                        } else {
                            // 普通模型响应（增量内容）
                            return assistantMessage.getText();
                        }
                    }
                } else if (streamingOutput.getOutputType() == OutputType.AGENT_MODEL_FINISHED) {
                    return ""; // 完成时不额外输出
                }
            }
            return "";
        });
    }
}