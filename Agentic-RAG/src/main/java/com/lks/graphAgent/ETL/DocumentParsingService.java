package com.lks.graphAgent.ETL;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.cloud.ai.document.DocumentParser;
import org.springframework.ai.document.Document;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class DocumentParsingService {
    public List<Document> parseDocument(String fileName) {
        // 创建 Tika Parser
        DocumentParser parser = new TikaDocumentParser();

        // 构建文件路径，指向files目录
        Path filePath = Paths.get("files", fileName);
        Resource resource = new FileSystemResource(filePath.toString());
        
        try (InputStream inputStream = resource.getInputStream()) {
            // 解析文档
            return parser.parse(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("无法读取文件: " + fileName, e);
        }
    }
}