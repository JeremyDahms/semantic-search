package com.jdahms.semantic_search.service;

import com.jdahms.semantic_search.exception.OllamaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);

    private final String ollamaApiUrl;
    private final String modelName;
    private final RestTemplate restTemplate;

    public OllamaService(RestTemplate restTemplate,
                         @Value("${ollama.api.url:http://ollama:11434/api/embeddings}") String ollamaApiUrl,
                         @Value("${ollama.model.name:nomic-embed-text}") String modelName) {
        this.restTemplate = restTemplate;
        this.ollamaApiUrl = ollamaApiUrl;
        this.modelName = modelName;
    }

    public float[] generateEmbedding(String text) {
       OllamaRequest request = new OllamaRequest(modelName, text);

       try {
           OllamaResponse response = restTemplate.postForObject(
                   ollamaApiUrl,
                   request,
                   OllamaResponse.class
           );

           if (response == null || response.embedding() == null) {
               logger.error("Received null response from Ollama API for text: {}", text.substring(0, Math.min(50, text.length())));
               throw new OllamaApiException("Failed to generate embedding: Ollama returned null response");
           }

           return response.embedding();
       } catch (OllamaApiException e) {
           throw e;
       } catch (Exception e) {
           logger.error("Error calling Ollama API at {}", ollamaApiUrl, e);
           throw new OllamaApiException("Failed to generate embedding from Ollama", e);
       }
    }

    public String embeddingToString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return "[]";
        }

        // Pre-size StringBuilder: estimate ~10 chars per float + commas + brackets
        StringBuilder sb = new StringBuilder(embedding.length * 10 + 2);
        sb.append('[');
        sb.append(embedding[0]);

        for (int i = 1; i < embedding.length; i++) {
            sb.append(',');
            sb.append(embedding[i]);
        }

        sb.append(']');
        return sb.toString();
    }

    record OllamaRequest(String model, String prompt) {}

    record OllamaResponse(float[] embedding) {}
}
