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
            String port = environment.getProperty("server.port", "8001");
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
