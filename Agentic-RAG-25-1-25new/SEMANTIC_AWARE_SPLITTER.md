# 语义感知文本分割器 (SemanticAwareTextSplitter)

## 概述

语义感知文本分割器是一个先进的文本分割工具，结合了递归字符分割和语义相似性聚类的策略。它不仅考虑了文本的结构特征，还利用语义信息来决定如何最佳地分割文本，以保持语义连贯性。

## 主要特性

### 1. 递归字符分割
- 使用多层分隔符策略（如段落、句子、标点符号、空格等）
- 按预设的块大小和重叠参数进行初步分割
- 保证分割后的文本块在语法上尽可能完整

### 2. 语义相似性聚类
- 基于词汇重叠和上下文信息计算文本块间的语义相似度
- 使用Jaccard相似度和位置权重相结合的方法
- 支持可配置的相似度阈值，决定哪些块可以合并

### 3. 灵活的配置选项
- `chunkSize`: 每个块的最大大小
- `chunkOverlap`: 块之间的重叠大小
- `separators`: 用于分割的分隔符数组
- `similarityThreshold`: 语义相似性阈值（0.0-1.0）
- `maxClusterSize`: 最大聚类大小，即最多合并多少个相邻块

## 实现细节

### 递归分割算法
1. 首先尝试使用高级分隔符（如段落分隔符）
2. 如果文本仍太大，使用低级分隔符（如句子结束符）
3. 最后使用固定大小分割作为后备方案

### 语义聚类算法
1. 计算相邻文本块的语义相似度
2. 如果相似度超过阈值且合并后不超过大小限制，则合并
3. 限制最大聚类大小以防止块过大

### 相似度计算
- 使用Jaccard相似度计算词汇重叠
- 考虑文本开头的词汇匹配（位置权重）
- 组合两种权重得出最终相似度分数

## 使用示例

### 基础使用
```java
SemanticAwareTextSplitter splitter = new SemanticAwareTextSplitter();
List<String> chunks = splitter.splitTextInput(text);
```

### 自定义配置
```java
SemanticAwareTextSplitter splitter = new SemanticAwareTextSplitter(
    800, 100,                    // 块大小和重叠
    new String[]{"\n\n", "\n", "。", "，"}, // 分隔符
    0.6,                         // 相似度阈值
    4                            // 最大聚类大小
);
```

### 在RAG应用中使用
```java
@Component
public class RAGDocumentProcessor {
    private final SemanticAwareTextSplitter textSplitter = 
        new SemanticAwareTextSplitter(800, 100, 
            new String[]{"\n\n", "\n", "。", "！", "？", "；", "，", " "}, 
            0.6, 4);

    public void processDocumentForRAG(InputStream inputStream) {
        List<Document> documents = parser.parse(inputStream);
        List<Document> splitDocuments = textSplitter.transform(documents);
        // ... 继续处理
    }
}
```

## 优势

1. **语义连贯性**: 保持文本块内部的语义一致性
2. **灵活配置**: 可根据不同应用场景调整参数
3. **性能优化**: 在保持语义的同时优化分割效率
4. **渐进式**: 即使没有向量存储也能使用基于规则的语义分析
5. **兼容性**: 继承自Spring AI的TextSplitter，与现有生态系统兼容

## 应用场景

- RAG (Retrieval Augmented Generation) 系统中的文档预处理
- 长文档的智能分割
- 保持上下文连贯性的文本处理
- 需要语义相关性的知识库构建