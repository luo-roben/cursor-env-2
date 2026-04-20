package com.review.infrastructure.vector.impl;

import com.review.infrastructure.vector.VectorStoreService;
import com.review.infrastructure.vector.dto.VectorSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("milvusVectorStoreService")
@ConditionalOnProperty(name = "review.milvus.enabled", havingValue = "true")
public class MilvusVectorStoreService implements VectorStoreService {

    public MilvusVectorStoreService() {
        log.info("Milvus vector store service initialized (stub - real Milvus SDK integration placeholder)");
    }

    @Override
    public List<VectorSearchResult> search(String text, String collection, int topK) {
        try {
            log.debug("Milvus search in collection={}, topK={}", collection, topK);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Milvus search failed, returning empty results: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void index(String id, String text, String collection, Map<String, Object> metadata) {
        try {
            log.debug("Milvus index id={} in collection={}", id, collection);
        } catch (Exception e) {
            log.warn("Milvus index failed: {}", e.getMessage());
        }
    }
}
