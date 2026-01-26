package com.lks.graphAgent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled("localhost", 6379);
    }
    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("custom-index")                // 可选：默认为 "spring-ai-index"
                .prefix("custom-prefix")                  // 可选：默认为 "embedding:"
                .initializeSchema(true)                   // 可选：默认为 false
                .build();
    }
}
