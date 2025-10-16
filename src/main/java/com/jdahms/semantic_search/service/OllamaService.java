package com.jdahms.semantic_search.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    private final String ollamaApiUrl;
    private final RestTemplate restTemplate;

    public OllamaService(RestTemplate restTemplate,
                         @Value("${ollama.api.url:http://ollama:11434/api/embeddings}") String ollamaApiUrl) {
        this.restTemplate = restTemplate;
        this.ollamaApiUrl = ollamaApiUrl;
    }

    public float[] generateEmbedding(String text) {
       OllamaRequest request = new OllamaRequest("nomic-embed-text", text);

       OllamaResponse response = restTemplate.postForObject(
               ollamaApiUrl,
               request,
               OllamaResponse.class
       );

       return response != null ? response.embedding() : new float[0];
    }

    public String embeddingToString(float[] embedding) {
       StringBuilder sb = new StringBuilder("[");
       for (int i = 0; i < embedding.length; i++) {
           sb.append(embedding[i]);
           if (i != embedding.length - 1) {
               sb.append(",");
           }
       }
       sb.append("]");
         return sb.toString();
    }

    record OllamaRequest(String model, String prompt) {}

    record OllamaResponse(float[] embedding) {}
}
