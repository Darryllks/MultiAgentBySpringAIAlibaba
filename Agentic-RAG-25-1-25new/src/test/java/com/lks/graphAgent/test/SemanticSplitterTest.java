package com.lks.graphAgent.test;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.lks.graphAgent.transformer.splitter.SemanticAwareTextSplitter;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SemanticAwareTextSplitter 测试类
 * 用于测试引入EmbeddingModel后的语义分割效果
 */
@SpringBootApplication
public class SemanticSplitterTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 创建EmbeddingModel的Bean配置示例（实际应用中需要配置正确的API密钥）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        String dashScopeApiKey = "sk-fddc34d562ab49059c2d35c5fb1e0cc6";
        DashScopeApi apikey = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        EmbeddingModel embeddingModel = new DashScopeEmbeddingModel(apikey);
        return embeddingModel;
    }

    public static void main(String[] args) {
        // 这里展示如何创建测试实例，实际运行需要Spring Boot环境和有效的API密钥
        System.out.println("=== SemanticAwareTextSplitter 带EmbeddingModel测试 ===\n");

        // 示例：测试不同配置下的分割效果
        testWithMockEmbeddingModel();
    }

    /**
     * 使用模拟EmbeddingModel进行测试
     * 注意：实际部署时需要替换为真实可用的EmbeddingModel
     */
    private static void testWithMockEmbeddingModel() {
        System.out.println("\n=== 测试带EmbeddingModel的语义分割功能 ===");

        String sampleText = """
            人工智能是计算机科学的一个重要分支，致力于创建能够模拟、延伸甚至超越人类智能的系统。
            它涵盖了多个领域，包括机器学习、深度学习、自然语言处理、计算机视觉等。
            
            机器学习是人工智能的核心技术之一，它使计算机能够在没有明确编程的情况下学习和改进。
            通过大量数据的训练，机器学习模型可以识别模式、做出预测并做出决策。
            
            深度学习是机器学习的一个子集，它模仿人脑的神经网络结构来处理复杂的数据。
            深度学习在图像识别、语音识别等领域取得了突破性进展。
            
            自然语言处理关注计算机与人类语言之间的交互，使机器能够理解、解释和生成人类语言。
            这项技术广泛应用于聊天机器人、翻译系统和文本分析工具中。
            """;

        try {
            //创建EmbeddingModel实例（需要有效的API密钥）
            String dashScopeApiKey = "sk-fddc34d562ab49059c2d35c5fb1e0cc6";
            DashScopeApi apikey = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
            EmbeddingModel embeddingModel = new DashScopeEmbeddingModel(apikey);

            // 创建带EmbeddingModel的分割器
            SemanticAwareTextSplitter semanticSplitter = new SemanticAwareTextSplitter(50, 10,
                    new String[]{"\n\n", "。", "！", "？"}, 0.6, 3, embeddingModel);

            List<String> semanticChunks = semanticSplitter.splitTextInput(sampleText);

            assertNotNull(semanticChunks);
            assertTrue(semanticChunks.size() > 0, "应该至少有一个文本块");
            System.out.println("语义分割结果: " + semanticChunks.size() + " 个块");

            for (int i = 0; i < semanticChunks.size(); i++) {
                String chunk = semanticChunks.get(i);
                System.out.printf("语义块 %d (长度: %d): \"%s\"\n",
                        i + 1, chunk.length(), truncateText(chunk, 100));
            }
        } catch (Exception e) {
            System.err.println("EmbeddingModel测试失败: " + e.getMessage());
        }
    }

    /**
     * 显示分块结果
     */
    private static void displayChunks(List<String> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            System.out.printf("块 %d (长度: %d):\n", i + 1, chunk.length());
            System.out.println("\"" + truncateText(chunk, 150) + "\"\n");
        }
        System.out.printf("总共 %d 个块\n", chunks.size());
    }

    /**
     * 截断文本以适应显示
     */
    private static String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

}