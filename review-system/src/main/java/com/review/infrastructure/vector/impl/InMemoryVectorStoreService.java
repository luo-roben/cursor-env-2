package com.review.infrastructure.vector.impl;

import com.review.infrastructure.vector.VectorStoreService;
import com.review.infrastructure.vector.dto.VectorSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "milvusVectorStoreService")
public class InMemoryVectorStoreService implements VectorStoreService {

    private final Map<String, Map<String, VectorEntry>> store = new ConcurrentHashMap<>();

    @Override
    public List<VectorSearchResult> search(String text, String collection, int topK) {
        Map<String, VectorEntry> collectionStore = store.get(collection);
        if (collectionStore == null || collectionStore.isEmpty()) {
            return Collections.emptyList();
        }

        double[] queryVec = simpleTextToVector(text);

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
                .put(id, new VectorEntry(id, simpleTextToVector(text), metadata));
        log.debug("Indexed document {} in collection {}", id, collection);
    }

    private double[] simpleTextToVector(String text) {
        double[] vec = new double[64];
        if (text == null || text.isEmpty()) return vec;
        for (int i = 0; i < text.length(); i++) {
            vec[i % 64] += text.charAt(i);
        }
        double norm = 0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
        return vec;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private record VectorEntry(String id, double[] vector, Map<String, Object> metadata) {}
}
