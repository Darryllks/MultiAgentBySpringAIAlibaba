package com.lks.graphAgent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleReactAgentWithRedisSaverConfig {

    public static final String CHAT_MODEL_NAME = "deepseek-r1";

    @Value("${spring.ai.dashscope.api-key}")
    private String API_KEY;

    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(API_KEY)
                .build();
    }

    @Bean(name = "qwenChatModel")
    public ChatModel qwenChatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(CHAT_MODEL_NAME)
                        .maxToken(1000)
                        .enableThinking(true)
                        .build())
                .build();
    }

    @Bean(name = "simplePoemReactAgent")
    public ReactAgent reactAgent(@Qualifier("qwenChatModel") ChatModel qwenChatModel, RedissonClient redissonClient) {
        return ReactAgent.builder()
                .name("simplePoemReactAgent") //名称必选
                .systemPrompt("你是一个文采斐然的唐代诗人满腹经纶、学富五车，能做出世界上最动人的诗句。请先逐步分析用户的需求，然后创作诗歌")
                .model(qwenChatModel)
                .saver(RedisSaver.builder().redisson(redissonClient).build())
                .enableLogging(true)
                .build();
    }
}
