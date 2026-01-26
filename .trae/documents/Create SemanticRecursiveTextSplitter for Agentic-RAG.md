I will create a new class `SemanticRecursiveTextSplitter` in the `com.lks.graphAgent.FileIngestion` package.
This class will extend `org.springframework.ai.transformer.splitter.TextSplitter` and implement the requested functionality: combining recursive splitting with semantic similarity clustering.

### **Plan Details:**

1.  **Create `SemanticRecursiveTextSplitter.java`**:
    - **Location**: `Agentic-RAG-25-1-25new/src/main/java/com/lks/graphAgent/FileIngestion/SemanticRecursiveTextSplitter.java`
    - **Inheritance**: Extends `org.springframework.ai.transformer.splitter.TextSplitter`.
    - **Dependencies**: 
        - `org.springframework.ai.embedding.EmbeddingModel` (for semantic calculation)
        - `org.springframework.ai.transformer.splitter.RecursiveCharacterTextSplitter` (for initial fine-grained splitting)
    - **Core Logic**:
        - **Step 1 (Recursive Split)**: Use `RecursiveCharacterTextSplitter` to break the document into small, manageable chunks (e.g., sentences or small paragraphs).
        - **Step 2 (Embedding)**: Generate vector embeddings for these chunks using the `EmbeddingModel`.
        - **Step 3 (Clustering & Merging)**: Iterate through the chunks and calculate Cosine Similarity between adjacent chunks. If the similarity exceeds a defined threshold (e.g., 0.8), merge them into a single chunk.
        - **Step 4 (Output)**: Return the re-grouped documents.

2.  **Implementation Details**:
    - The class will have a constructor accepting `EmbeddingModel`, `similarityThreshold` (default 0.8), and `minChunkSize` parameters.
    - It will override the `split(Document document)` method (or `apply` method depending on the exact API of `TextSplitter` in the used version) to execute the logic.

3.  **Verification**:
    - I will assume the standard Spring AI 1.0.0+ API is in use (`apply` or `split` methods).
    - I will rely on the existing `RecursiveCharacterTextSplitter` for the "recursive" part to ensure robustness.

This new class will allow you to split documents based on actual semantic meaning rather than just character counts, improving RAG retrieval quality.
