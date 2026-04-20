package com.review.infrastructure.search.impl;

import com.review.infrastructure.search.FullTextSearchService;
import com.review.infrastructure.search.dto.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("elasticsearchSearchService")
@ConditionalOnProperty(name = "review.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchSearchService implements FullTextSearchService {

    public ElasticsearchSearchService() {
        log.info("Elasticsearch search service initialized (stub - real ES integration placeholder)");
    }

    @Override
    public List<SearchResult> search(String query, String index, int topK) {
        try {
            log.debug("ES search in index={}, topK={}", index, topK);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("ES search failed, returning empty results: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void index(String id, String content, String index, Map<String, Object> metadata) {
        try {
            log.debug("ES index id={} in index={}", id, index);
        } catch (Exception e) {
            log.warn("ES index failed: {}", e.getMessage());
        }
    }
}
