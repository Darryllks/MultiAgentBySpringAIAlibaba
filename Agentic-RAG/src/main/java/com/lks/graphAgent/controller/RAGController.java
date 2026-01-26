package com.lks.graphAgent.controller;

import com.lks.graphAgent.ETLPipeline.FileIngestion.RAGDocumentProcessor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RAGController {

    private final RAGDocumentProcessor ragDocumentProcessor;
    private final ResourcePatternResolver resourcePatternResolver;

    @Autowired
    public RAGController(RAGDocumentProcessor ragDocumentProcessor, ResourcePatternResolver resourcePatternResolver) {
        this.ragDocumentProcessor = ragDocumentProcessor;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @GetMapping("/ingest")
    public Map<String, Object> ingestFiles() {
        Map<String, Object> result = new HashMap<>();
        List<String> processedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Load all files from the 'files' directory in resources
            Resource[] resources = resourcePatternResolver.getResources("classpath*:files/*");
            
            for (Resource resource : resources) {
                // Skip if it is a directory (though classpath usually returns files)
                if (resource.exists() && resource.isReadable()) {
                    try {
                        ragDocumentProcessor.processDocumentForRAG(resource.getInputStream());
                        processedFiles.add(resource.getFilename());
                    } catch (Exception e) {
                        errors.add("Error processing " + resource.getFilename() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            errors.add("Failed to load resources: " + e.getMessage());
        }

        result.put("status", errors.isEmpty() ? "success" : "partial_success");
        result.put("processed_files", processedFiles);
        result.put("errors", errors);
        return result;
    }

    @GetMapping("/search")
    public List<Document> search(@RequestParam(name = "query") String query) {
        return ragDocumentProcessor.searchSimilarDocuments(query);
    }
}
