package com.review.infrastructure.graph;

import java.util.List;
import java.util.Map;

public interface KnowledgeGraphService {

    void addArticleRelation(Long fromArticleId, Long toArticleId, String relationType);

    List<Map<String, Object>> findRelatedArticles(Long articleId, int depth);

    void addClauseRelation(String fromClause, String toClause, String relationType);

    List<Map<String, Object>> findClauseRelations(String clauseId, int depth);
}
