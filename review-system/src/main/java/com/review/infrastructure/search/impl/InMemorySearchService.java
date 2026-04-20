package com.review.infrastructure.search.impl;

import com.review.infrastructure.search.FullTextSearchService;
import com.review.infrastructure.search.dto.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "elasticsearchSearchService")
public class InMemorySearchService implements FullTextSearchService {

    private final Map<String, Map<String, DocEntry>> store = new ConcurrentHashMap<>();

    @Override
    public List<SearchResult> search(String query, String index, int topK) {
        Map<String, DocEntry> indexStore = store.get(index);
        if (indexStore == null || indexStore.isEmpty() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.toLowerCase();
        return indexStore.values().stream()
                .filter(e -> e.content.toLowerCase().contains(lowerQuery))
                .map(e -> SearchResult.builder()
                        .id(e.id)
                        .score(1.0)
                        .content(e.content)
                        .metadata(e.metadata)
                        .build())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public void index(String id, String content, String index, Map<String, Object> metadata) {
        store.computeIfAbsent(index, k -> new ConcurrentHashMap<>())
                .put(id, new DocEntry(id, content, metadata));
        log.debug("Indexed document {} in index {}", id, index);
    }

    private record DocEntry(String id, String content, Map<String, Object> metadata) {}
}
