package com.lks.graphAgent.transformer.splitter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticAwareTextSplitterTest {

    @Test
    public void testBasicSplitting() {
        SemanticAwareTextSplitter splitter = new SemanticAwareTextSplitter();
        
        String text = "这是第一段内容。它包含一些相关信息。\n\n这是第二段内容。这部分与前面的内容有所不同。";
        
        List<String> chunks = splitter.splitText(text);
        
        assertNotNull(chunks);
        assertTrue(chunks.size() > 0);
        System.out.println("分割得到 " + chunks.size() + " 个块:");
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("块 " + i + ": " + chunks.get(i).substring(0, Math.min(chunks.get(i).length(), 50)) + "...");
        }
    }

    @Test
    public void testDocumentSplitting() {
        SemanticAwareTextSplitter splitter = new SemanticAwareTextSplitter(500, 50, 
            new String[]{"\n\n", "\n", "。", "，"}, 0.6, 3);
        
        String text = "人工智能是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。" +
                     "该领域的研究包括机器人、语言识别、图像识别、自然语言处理和专家系统等。" +
                     "\n\n机器学习是人工智能的一个重要分支，它是一种通过算法解析数据、从中学习，然后对真实世界中的事件做出决策和预测的方法。" +
                     "与传统的使用特定指令集来执行特定任务的软件不同，机器学习使用大量的数据和算法来训练模型。" +
                     "\n\n深度学习是机器学习的一个子集，它模仿人脑的工作方式来创建人工神经网络。" +
                     "这些神经网络能够识别模式，并对未见过的数据进行分类，而无需人类干预。";
        
        Document document = new Document(text);
        List<Document> splitDocuments = splitter.split(Arrays.asList(document));
        
        assertNotNull(splitDocuments);
        assertTrue(splitDocuments.size() > 0);
        System.out.println("文档分割得到 " + splitDocuments.size() + " 个文档块:");
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document doc = splitDocuments.get(i);
            System.out.println("文档块 " + i + " (长度: " + doc.getText().length() + "): " + 
                             doc.getText().substring(0, Math.min(doc.getText().length(), 60)) + "...");
            
            // 检查元数据是否正确添加
            assertTrue(doc.getMetadata().containsKey("parent_document_id"));
            assertTrue(doc.getMetadata().containsKey("chunk_index"));
            assertTrue(doc.getMetadata().containsKey("total_chunks"));
        }
    }

    @Test
    public void testDifferentConfigurations() {
        // 测试不同的配置
        SemanticAwareTextSplitter smallSplitter = new SemanticAwareTextSplitter(100, 10, null, 0.5, 2);
        SemanticAwareTextSplitter largeSplitter = new SemanticAwareTextSplitter(2000, 200, null, 0.8, 5);
        
        String text = "这是一段较长的文本，用于测试不同配置的分割效果。" +
                     "我们会看到较小的块大小会产生更多的分割块。" +
                     "而较大的块大小则会产生较少但更大的块。" +
                     "\n\n语义相似性阈值也会影响分割结果。" +
                     "较高的阈值意味着只有非常相似的文本块才会被合并。" +
                     "较低的阈值则允许更多语义相关的块被合并。" +
                     "\n\n最后，最大聚类大小限制了可以合并的最大块数。";
        
        List<String> smallChunks = smallSplitter.splitText(text);
        List<String> largeChunks = largeSplitter.splitText(text);
        
        assertNotNull(smallChunks);
        assertNotNull(largeChunks);
        
        System.out.println("小块配置分割得到 " + smallChunks.size() + " 个块");
        System.out.println("大块配置分割得到 " + largeChunks.size() + " 个块");
        
        // 通常小块配置会产生更多块
        assertTrue(smallChunks.size() >= largeChunks.size());
    }

    @Test
    public void testEdgeCases() {
        SemanticAwareTextSplitter splitter = new SemanticAwareTextSplitter();
        
        // 测试空文本
        List<String> emptyResult = splitter.splitText("");
        assertEquals(0, emptyResult.size());
        
        // 测试短文本
        List<String> shortResult = splitter.splitText("短文本。");
        assertEquals(1, shortResult.size());
        
        // 测试单字符文本
        List<String> singleCharResult = splitter.splitText("A");
        assertEquals(1, singleCharResult.size());
    }

    @Test
    public void testConfigurationGetters() {
        SemanticAwareTextSplitter splitter = new SemanticAwareTextSplitter(800, 100, 
            new String[]{"\n\n", "。", "！"}, 0.75, 4);
        
        assertEquals(800, splitter.getChunkSize());
        assertEquals(100, splitter.getChunkOverlap());
        assertEquals(0.75, splitter.getSimilarityThreshold(), 0.01);
        assertEquals(4, splitter.getMaxClusterSize());
        assertNull(splitter.getVectorStore()); // 因为我们没有传入vectorStore
    }
}