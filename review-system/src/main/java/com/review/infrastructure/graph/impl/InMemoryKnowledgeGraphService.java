package com.review.infrastructure.graph.impl;

import com.review.infrastructure.graph.KnowledgeGraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "neo4jKnowledgeGraphService")
public class InMemoryKnowledgeGraphService implements KnowledgeGraphService {

    private final Map<String, List<EdgeEntry>> adjacencyList = new ConcurrentHashMap<>();

    @Override
    public void addArticleRelation(Long fromArticleId, Long toArticleId, String relationType) {
        String key = "article:" + fromArticleId;
        adjacencyList.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new EdgeEntry("article:" + toArticleId, relationType));
        log.debug("Added article relation: {} -> {} ({})", fromArticleId, toArticleId, relationType);
    }

    @Override
    public List<Map<String, Object>> findRelatedArticles(Long articleId, int depth) {
        String key = "article:" + articleId;
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(key);
        collectRelated(key, depth, visited, results);
        return results;
    }

    @Override
    public void addClauseRelation(String fromClause, String toClause, String relationType) {
        String key = "clause:" + fromClause;
        adjacencyList.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new EdgeEntry("clause:" + toClause, relationType));
        log.debug("Added clause relation: {} -> {} ({})", fromClause, toClause, relationType);
    }

    @Override
    public List<Map<String, Object>> findClauseRelations(String clauseId, int depth) {
        String key = "clause:" + clauseId;
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(key);
        collectRelated(key, depth, visited, results);
        return results;
    }

    private void collectRelated(String key, int depth, Set<String> visited, List<Map<String, Object>> results) {
        if (depth <= 0) return;
        List<EdgeEntry> edges = adjacencyList.getOrDefault(key, Collections.emptyList());
        for (EdgeEntry edge : edges) {
            if (!visited.contains(edge.target)) {
                visited.add(edge.target);
                Map<String, Object> result = new HashMap<>();
                result.put("id", edge.target);
                result.put("relationType", edge.relationType);
                results.add(result);
                collectRelated(edge.target, depth - 1, visited, results);
            }
        }
    }

    private record EdgeEntry(String target, String relationType) {}
}
