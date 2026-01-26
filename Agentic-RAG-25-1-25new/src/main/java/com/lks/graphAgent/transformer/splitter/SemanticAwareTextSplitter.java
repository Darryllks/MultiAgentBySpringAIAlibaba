package com.lks.graphAgent.transformer.splitter;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 语义感知文本分割器
 * 结合递归字符分割和语义相似性聚类的文档切分策略
 */
public class SemanticAwareTextSplitter extends TextSplitter {

    private final int chunkSize;
    private final int chunkOverlap;
    private final String[] separators;
    private final double similarityThreshold;
    private final int maxClusterSize;

    // 嵌入模型，用于计算语义相似性
    @Nullable
    private final EmbeddingModel embeddingModel;

    // 向量存储，作为备用
    @Nullable
    private final VectorStore vectorStore;

    /**
     * 使用默认参数创建语义感知文本分割器
     */
    public SemanticAwareTextSplitter() {
        this(1024, 200, null, 0.7, 5, null);
    }

    /**
     * 创建语义感知文本分割器（不使用EmbeddingModel或VectorStore）
     *
     * @param chunkSize           每个块的最大大小
     * @param chunkOverlap        块之间的重叠大小
     * @param separators          用于分割的分隔符数组
     * @param similarityThreshold 语义相似性阈值（0.0-1.0），低于此值的块将被合并
     * @param maxClusterSize      最大聚类大小，即最多合并多少个相邻块
     */
    public SemanticAwareTextSplitter(int chunkSize, int chunkOverlap, String[] separators, 
                                   double similarityThreshold, int maxClusterSize) {
        this(chunkSize, chunkOverlap, separators, similarityThreshold, maxClusterSize, null);
    }

    /**
     * 创建语义感知文本分割器
     *
     * @param chunkSize           每个块的最大大小
     * @param chunkOverlap        块之间的重叠大小
     * @param separators          用于分割的分隔符数组
     * @param similarityThreshold 语义相似性阈值（0.0-1.0），低于此值的块将被合并
     * @param maxClusterSize      最大聚类大小，即最多合并多少个相邻块
     * @param embeddingModel      嵌入模型，用于计算语义相似性
     */
    public SemanticAwareTextSplitter(int chunkSize, int chunkOverlap, String[] separators, 
                                   double similarityThreshold, int maxClusterSize, 
                                   @Nullable EmbeddingModel embeddingModel) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }

        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = separators != null ? separators : 
            new String[] { "\n\n", "\n", "。", "！", "？", "；", "，", " " };
        this.similarityThreshold = similarityThreshold;
        this.maxClusterSize = maxClusterSize;
        this.embeddingModel = embeddingModel;
        this.vectorStore = null; // 不再支持vectorStore参数
    }

    @Override
    protected List<String> splitText(String text) {
        // 第一步：使用递归字符分割算法初步分割文本
        System.out.println("第一步：使用递归字符分割算法初步分割文本");
        List<String> initialChunks = recursiveSplit(text);
        
        // 第二步：根据是否提供嵌入模型，选择不同的语义处理策略
        if (embeddingModel != null) {
            System.out.println("存在embeddingModel");
            return clusterSemanticallyWithEmbeddings(initialChunks);
        } else {
            System.out.println("没有找到embeddingModel");
            // 如果没有嵌入模型，则使用基于规则的语义感知合并
            return ruleBasedSemanticClustering(initialChunks);
        }
    }
    
    /**
     * 公开的splitText方法，允许外部访问
     */
    public List<String> splitTextInput(String text) {
        return splitText(text);
    }

    /**
     * 递归字符分割 - 初步分割文本
     */
//    private List<String> recursiveSplit(String text) {
//        List<String> chunks = new ArrayList<>();
//        recursiveSplitInternal(text, 0, chunks);
//        return chunks;
//    }
//
//    private void recursiveSplitInternal(String text, int separatorIndex, List<String> chunks) {
//        if (text.isEmpty()) {
//            return;
//        }
//
//        // 如果文本长度小于等于块大小，直接添加
//        if (text.length() <= chunkSize) {
//            chunks.add(text);
//            return;
//        }
//
//        // 如果已经尝试了所有分隔符，按固定大小分割
//        if (separatorIndex >= separators.length) {
//            // 使用滑动窗口进行分割，确保语义连续性
//            for (int i = 0; i < text.length(); i += chunkSize - chunkOverlap) {
//                int end = Math.min(i + chunkSize, text.length());
//                String chunk = text.substring(i, end);
//                if (!chunk.trim().isEmpty()) {
//                    chunks.add(chunk);
//                }
//                if (end == text.length()) {
//                    break;
//                }
//            }
//            return;
//        }
//
//        String separator = separators[separatorIndex];
//        String[] splits;
//
//        // 根据分隔符类型进行分割
//        if (separator.isEmpty()) {
//            // 按字符分割
//            splits = new String[text.length()];
//            for (int i = 0; i < text.length(); i++) {
//                splits[i] = String.valueOf(text.charAt(i));
//            }
//        } else {
//            splits = text.split(java.util.regex.Pattern.quote(separator));
//        }
//
//        // 重构文本并分割，确保语义完整性
//        int currentIndex = 0;
//        for (int i = 0; i < splits.length; i++) {
//            String currentSplit = splits[i];
//            int nextIndex = currentIndex + currentSplit.length();
//
//            // 如果当前分割部分太长，递归处理
//            if (currentSplit.length() > chunkSize) {
//                recursiveSplitInternal(currentSplit, separatorIndex + 1, chunks);
//            } else {
//                // 检查当前片段是否适合添加到现有块中
//                if (!currentSplit.trim().isEmpty()) {
//                    String segment = currentSplit + (i < splits.length - 1 ? separator : "");
//
//                    // 如果当前没有块或当前块加上新段会超出大小限制，创建新块
//                    if (chunks.isEmpty() ||
//                        chunks.get(chunks.size() - 1).length() + segment.length() > chunkSize * 1.2) {
//                        chunks.add(segment);
//                    } else {
//                        // 尝试将段添加到现有块中
//                        String lastChunk = chunks.get(chunks.size() - 1);
//                        if (lastChunk.length() + segment.length() <= chunkSize) {
//                            chunks.set(chunks.size() - 1, lastChunk + segment);
//                        } else {
//                            chunks.add(segment);
//                        }
//                    }
//                }
//            }
//
//            currentIndex = nextIndex + separator.length();
//        }
//    }
    private List<String> recursiveSplit(String text) {
        List<String> chunks = new ArrayList<>();

        // 使用句子分割逻辑替代原来的递归分割
        chunks.addAll(splitBySentences(text));

        return chunks;
    }

    /**
     * 按句子分割文本
     */
    private List<String> splitBySentences(String text) {
        List<String> sentences = new ArrayList<>();

        // 使用正则表达式按句号、问号、感叹号分割
        String[] potentialSentences = text.split("(?<=[。！？!?])\\s*");

        StringBuilder currentChunk = new StringBuilder();

        for (String potentialSentence : potentialSentences) {
            if (potentialSentence.trim().isEmpty()) {
                continue;
            }

            // 如果加上当前句子会超过大小限制
            if (currentChunk.length() + potentialSentence.length() > chunkSize && currentChunk.length() > 0) {
                // 保存当前块
                if (currentChunk.length() > 0) {
                    sentences.add(currentChunk.toString().trim());
                }
                // 开始新块
                currentChunk = new StringBuilder(potentialSentence);
            } else {
                // 添加到当前块
                if (currentChunk.length() > 0) {
                    currentChunk.append(potentialSentence);
                } else {
                    currentChunk.append(potentialSentence);
                }
            }
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            sentences.add(currentChunk.toString().trim());
        }

        // 过滤掉空字符串
        return sentences.stream()
                .filter(s -> !s.trim().isEmpty())
                .collect(ArrayList::new, (list, item) -> {
                    // 确保每个句子不超过最大大小
                    if (item.length() <= chunkSize) {
                        list.add(item);
                    } else {
                        // 如果单个句子超过大小限制，按字符分割
                        list.addAll(forceSplitLongSentence(item));
                    }
                }, ArrayList::addAll);
    }

    /**
     * 强制分割超长句子
     */
    private List<String> forceSplitLongSentence(String sentence) {
        List<String> parts = new ArrayList<>();

        for (int i = 0; i < sentence.length(); i += chunkSize - chunkOverlap) {
            int end = Math.min(i + chunkSize, sentence.length());
            String part = sentence.substring(i, end);

            if (!part.trim().isEmpty()) {
                parts.add(part);
            }

            // 如果到达末尾，退出循环
            if (end == sentence.length()) {
                break;
            }
        }

        return parts;
    }

    /**
     * 检查两个文本块是否语义相关
     */
    private boolean isSemanticallyRelated(String text1, String text2) {
        // 如果有嵌入模型，使用嵌入向量计算相似度
        if (embeddingModel != null) {
            try {
                System.out.println("基于embeddingModel计算嵌入相似度");
                double similarity = calculateEmbeddingSimilarity(text1, text2);
                System.out.println("相似度: " + similarity + ", 阈值: 0.5");
                return similarity >= 0.5; // 使用更合理的阈值作为语义相关判断
            } catch (Exception e) {
                // 如果嵌入模型计算失败，回退到基于规则的方法
                System.out.println("基于规则计算相似度");
                return isSemanticallyRelatedRuleBased(text1, text2);
            }
        } else {
            // 如果没有嵌入模型，使用基于规则的方法
            System.out.println("基于规则计算相似度");
            return isSemanticallyRelatedRuleBased(text1, text2);
        }
    }

    /**
     * 基于规则的语义相关性检查
     */
    private boolean isSemanticallyRelatedRuleBased(String text1, String text2) {
        // 检查共同词汇
        Set<String> words1 = extractMeaningfulWords(text1);
        Set<String> words2 = extractMeaningfulWords(text2);
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return false;
        }
        
        // 计算词汇重叠率
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        double overlapRatio = (double) intersection.size() / Math.max(words1.size(), words2.size());
        return overlapRatio > 0.1; // 至少10%的词汇重叠
    }

    /**
     * 基于嵌入模型的语义聚类合并
     */
    private List<String> clusterSemanticallyWithEmbeddings(List<String> initialChunks) {
        if (initialChunks.size() <= 1 || embeddingModel == null) {
            return ruleBasedSemanticClustering(initialChunks);
        }

        List<String> clusteredChunks = new ArrayList<>();
        int i = 0;
        
        while (i < initialChunks.size()) {
            StringBuilder currentCluster = new StringBuilder(initialChunks.get(i));
            int clusterCount = 1;

            System.out.println("开始尝试合并" );
            // 尝试将接下来的块与当前块合并，如果语义相似
            while (i + 1 < initialChunks.size() && 
                   clusterCount < maxClusterSize) {
                
                String nextChunk = initialChunks.get(i + 1);
                System.out.println("尝试合并块...");
                // 计算语义相似度
                double similarity = calculateEmbeddingSimilarity(currentCluster.toString(), nextChunk);

                System.out.println("检查合并后是否会超过大小限制");
                // 检查合并后是否会超过大小限制
                if (currentCluster.length() + nextChunk.length() <= chunkSize * 1.2) { // 增加大小限制的灵活性
                    if (similarity >= similarityThreshold) {
                        System.out.println("相似度高，合并！");
                        // 如果相似度高于阈值，则合并
                        currentCluster.append("\n").append(nextChunk);
                        i++; // 跳过下一个块，因为它已被合并
                        clusterCount++;
                    } else {
                        System.out.println("相似度不够，停止合并");
                        break; // 相似度不够，停止合并
                    }
                } else {
                    System.out.println("合并后超出大小限制，停止合并");
                    break; // 合并后会超过大小限制
                }
            }
            
            clusteredChunks.add(currentCluster.toString());
            i++;
        }
        
        return clusteredChunks;
    }

    /**
     * 基于嵌入模型计算两个文本块之间的语义相似度（余弦相似度）
     */
    public double calculateEmbeddingSimilarity(String text1, String text2) {
        if (embeddingModel == null) {
            // 如果没有嵌入模型，回退到基于规则的方法
            return ruleBasedSimilarity(text1, text2);
        }

        try {
            // 生成两个文本的嵌入向量
            float[] embedding1 = embeddingModel.embed(text1);
            float[] embedding2 = embeddingModel.embed(text2);

            return cosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            // 如果嵌入模型计算失败，回退到基于规则的方法
            System.err.println("Embedding calculation failed: " + e.getMessage());
            e.printStackTrace();
            return ruleBasedSimilarity(text1, text2);
        }
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            double val1 = vector1[i];
            double val2 = vector2[i];
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        if (norm1 == 0.0 && norm2 == 0.0) {
            return 1.0; // 两个零向量被认为是完全相似的
        }
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0; // 一个零向量和非零向量被认为是完全不相似的
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 基于规则的相似度计算
     */
    private double ruleBasedSimilarity(String text1, String text2) {
        // 基于共同词汇的相似度计算
        Set<String> words1 = extractMeaningfulWords(text1.toLowerCase());
        Set<String> words2 = extractMeaningfulWords(text2.toLowerCase());
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        // 使用Jaccard相似度
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        double jaccardSimilarity = (double) intersection.size() / union.size();
        
        // 考虑位置因素：如果文本开头有共同词，增加权重
        double positionalWeight = calculatePositionalWeight(text1, text2);
        
        return jaccardSimilarity * 0.7 + positionalWeight * 0.3;
    }

    /**
     * 计算位置权重（开头词语相似度）
     */
    private double calculatePositionalWeight(String text1, String text2) {
        // 提取开头的一些词
        String[] words1 = text1.toLowerCase().split("[\\s\\p{Punct}]+");
        String[] words2 = text2.toLowerCase().split("[\\s\\p{Punct}]+");
        
        Set<String> startWords1 = new HashSet<>();
        Set<String> startWords2 = new HashSet<>();
        
        // 取前几个词
        int startLimit = Math.min(5, Math.min(words1.length, words2.length));
        for (int i = 0; i < startLimit; i++) {
            if (i < words1.length && !words1[i].trim().isEmpty()) {
                startWords1.add(words1[i].trim());
            }
            if (i < words2.length && !words2[i].trim().isEmpty()) {
                startWords2.add(words2[i].trim());
            }
        }
        
        if (startWords1.isEmpty() || startWords2.isEmpty()) {
            return 0.0;
        }
        
        startWords1.retainAll(startWords2);
        return (double) startWords1.size() / Math.max(startWords1.size(), startWords2.size());
    }

    /**
     * 基于规则的语义聚类合并（不使用向量存储时的策略）
     */
    private List<String> ruleBasedSemanticClustering(List<String> initialChunks) {
        if (initialChunks.size() <= 1) {
            return initialChunks;
        }

        List<String> resultChunks = new ArrayList<>();
        int i = 0;
        
        while (i < initialChunks.size()) {
            StringBuilder currentChunk = new StringBuilder(initialChunks.get(i));
            int clusterCount = 1;
            
            // 尝试将后续块合并到当前块中，如果语义相关
            while (i + 1 < initialChunks.size() && 
                   clusterCount < maxClusterSize) {
                
                String nextChunk = initialChunks.get(i + 1);

                System.out.println("检查合并后是否会超过大小限制");
                // 检查合并后是否会超过大小限制 - 使用更合理的限制，允许一定比例的超出
                int maxSizeAllowed = Math.max(chunkSize * 2, chunkSize + chunkOverlap * 2); // 允许更大范围的大小限制
                // 检查合并后是否会超过大小限制
                if (currentChunk.length() + nextChunk.length() <= maxSizeAllowed) { // 增加大小限制的灵活性
                    // 检查语义相关性
                    System.out.println("检查语义相关性");
                    if (isSemanticallyRelated(currentChunk.toString(), nextChunk)) {
                        currentChunk.append("\n").append(nextChunk);
                        i++; // 跳过下一个块
                        clusterCount++;
                    } else {
                        break; // 语义不相关，停止合并
                    }
                } else {
                    System.out.println("检查大小超限");
                    break; // 大小超限，停止合并
                }
            }
            
            resultChunks.add(currentChunk.toString());
            i++;
        }
        
        return resultChunks;
    }

    /**
     * 提取有意义的词汇（去除停用词）
     */
    private Set<String> extractMeaningfulWords(String text) {
        // 简单的停用词列表
        Set<String> stopWords = Set.of("的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", 
                                      "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by");
        
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
                    .filter(word -> !word.isEmpty() && word.length() > 1 && !stopWords.contains(word))
                    .collect(Collectors.toSet());
    }

    // Getter方法
    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getMaxClusterSize() {
        return maxClusterSize;
    }
    
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
    
    public VectorStore getVectorStore() {
        return vectorStore;
    }
}