package com.review.infrastructure.vector.impl;

import com.review.infrastructure.embedding.EmbeddingService;
import com.review.infrastructure.vector.VectorStoreService;
import com.review.infrastructure.vector.dto.VectorSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "milvusVectorStoreService")
public class InMemoryVectorStoreService implements VectorStoreService {

    @Autowired
    private EmbeddingService embeddingService;

    private final Map<String, Map<String, VectorEntry>> store = new ConcurrentHashMap<>();

    @Override
    public List<VectorSearchResult> search(String text, String collection, int topK) {
        Map<String, VectorEntry> collectionStore = store.get(collection);
        if (collectionStore == null || collectionStore.isEmpty()) {
            return Collections.emptyList();
        }

        float[] queryVec = embeddingService.embed(text);

        return collectionStore.values().stream()
                .map(entry -> VectorSearchResult.builder()
                        .id(entry.id)
                        .score(cosineSimilarity(queryVec, entry.vector))
                        .metadata(entry.metadata)
                        .build())
                .sorted(Comparator.comparingDouble(VectorSearchResult::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public void index(String id, String text, String collection, Map<String, Object> metadata) {
        store.computeIfAbsent(collection, k -> new ConcurrentHashMap<>())
                .put(id, new VectorEntry(id, embeddingService.embed(text), metadata));
        log.debug("Indexed document {} in collection {}", id, collection);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private record VectorEntry(String id, float[] vector, Map<String, Object> metadata) {}
}
