package com.lks.graphAgent.ETLPipeline.FileIngestion;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class RAGDocumentProcessor {

    private final VectorStore vectorStore;
    private final TikaDocumentParser parser;
    // 用递归文本分割器，根据分隔符，自然切割以保持语义
    private final RecursiveCharacterTextSplitter textSplitter = new RecursiveCharacterTextSplitter();

    public RAGDocumentProcessor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.parser = new TikaDocumentParser();
    }

    public void processDocumentForRAG(InputStream inputStream) {
        try {
            // 1. 解析文档
            List<Document> documents = parser.parse(inputStream);

            // 2. 文本分割（将大文档分割成小块）
            List<Document> splitDocuments = textSplitter.transform(documents);

            // 3. 分批存储到向量数据库，避免超过API限制
            int batchSize = 5; // 设置批次大小，API限制为10，设置为5更安全
            for (int i = 0; i < splitDocuments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, splitDocuments.size());
                List<Document> batch = splitDocuments.subList(i, endIndex);
                vectorStore.write(batch);
                System.out.println("已处理第 " + (i/batchSize + 1) + " 批，共 " + batch.size() + " 个文档块");
            }

            System.out.println("成功加载 " + splitDocuments.size() + " 个文档块到向量数据库");
        } catch (Exception e) {
            System.err.println("处理文档时发生错误: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让调用者知道发生了错误
        }
    }

    public List<Document> searchSimilarDocuments(String query) {
        // 从向量数据库检索相似文档
        return vectorStore.similaritySearch(query);
    }
}
