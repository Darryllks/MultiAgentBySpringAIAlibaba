package com.lks.agent.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpRequest;

@Configuration
public class mcpConfig {
    /**
     * MCP同步HTTP客户端请求自定义器Bean
     *
     * 配置MCP(Model Context Protocol)客户端的HTTP请求头和超时设置。
     * 主要用于与Jina AI服务进行集成，提供搜索等外部工具支持。
     *
     * @return McpSyncHttpClientRequestCustomizer 实例
     */
    @Bean
    public McpSyncHttpClientRequestCustomizer mcpSyncHttpClientRequestCustomizer() {
        return new McpSyncHttpClientRequestCustomizer() {
            /**
             * 自定义HTTP请求
             *
             * @param builder HTTP请求构建器
             * @param method HTTP方法
             * @param endpoint 请求端点URI
             * @param body 请求体
             * @param context MCP传输上下文
             */
            @Override
            public void customize(HttpRequest.Builder builder, String method, URI endpoint, String body, McpTransportContext context) {
                // 添加Jina API密钥到Authorization头部
                builder.header("Authorization", "Bearer " + System.getenv("JINA_API_KEY"));
                // 设置请求超时时间为120秒
                builder.timeout(java.time.Duration.ofSeconds(120));
            }
        };
    }
}
