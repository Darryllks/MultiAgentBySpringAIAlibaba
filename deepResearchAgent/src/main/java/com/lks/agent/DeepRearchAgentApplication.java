package com.lks.agent;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpRequest;

@SpringBootApplication
public class DeepRearchAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeepRearchAgentApplication.class, args);
    }

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

    /**
     * 应用准备就绪事件监听器Bean
     *
     * 当Spring Boot应用完全启动后执行，用于显示访问信息。
     *
     * @param environment Spring环境对象，用于获取配置属性
     * @return ApplicationListener<ApplicationReadyEvent> 监听器实例
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener(Environment environment) {
        return event -> {
            // 获取服务器端口，默认8080
            String port = environment.getProperty("server.port", "8080");
            // 获取上下文路径，默认为空
            String contextPath = environment.getProperty("server.servlet.context-path", "");
            // 构建聊天界面访问URL
            String accessUrl = "http://localhost:" + port + contextPath + "/chatui/index.html";

            // 打印启动成功信息和访问链接
            System.out.println("\n🎉========================================🎉");
            System.out.println("✅ Application is ready!");
            System.out.println("🚀 Chat with you agent: " + accessUrl);
            System.out.println("🎉========================================🎉\n");
        };
    }
}
