package com.review.infrastructure.graph.impl;

import com.review.infrastructure.graph.KnowledgeGraphService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("neo4jKnowledgeGraphService")
@ConditionalOnProperty(name = "review.neo4j.enabled", havingValue = "true")
public class Neo4jKnowledgeGraphService implements KnowledgeGraphService {

    @Value("${review.neo4j.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${review.neo4j.username:neo4j}")
    private String username;

    @Value("${review.neo4j.password:neo4j123}")
    private String password;

    private Driver driver;
    private volatile boolean available = false;

    @PostConstruct
    public void init() {
        try {
            driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
            driver.verifyConnectivity();
            available = true;
            log.info("Neo4j knowledge graph service connected to {}", uri);
        } catch (Exception e) {
            log.warn("Failed to connect to Neo4j at {} - service will be unavailable: {}",
                    uri, e.getMessage());
            if (driver != null) {
                try {
                    driver.close();
                } catch (Exception ignored) {
                }
                driver = null;
            }
            available = false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                log.warn("Error closing Neo4j driver: {}", e.getMessage());
            }
        }
    }

    @Override
    public void addArticleRelation(Long fromArticleId, Long toArticleId, String relationType) {
        if (!available) {
            log.warn("Neo4j unavailable, skipping addArticleRelation: {} -> {} ({})",
                    fromArticleId, toArticleId, relationType);
            return;
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                        "MERGE (a:Article {articleId: $from}) " +
                        "MERGE (b:Article {articleId: $to}) " +
                        "MERGE (a)-[r:RELATES_TO {type: $relType}]->(b)",
                        Values.parameters("from", fromArticleId, "to", toArticleId, "relType", relationType)
                );
                return null;
            });
            log.debug("Neo4j addArticleRelation: {} -> {} ({})", fromArticleId, toArticleId, relationType);
        } catch (Exception e) {
            log.warn("Neo4j addArticleRelation failed: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> findRelatedArticles(Long articleId, int depth) {
        if (!available) {
            log.debug("Neo4j unavailable, returning empty results for findRelatedArticles: id={}", articleId);
            return Collections.emptyList();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(
                        "MATCH (a:Article {articleId: $id})-[r*1.." + Math.max(1, depth) + "]->(b:Article) " +
                        "RETURN DISTINCT b.articleId AS articleId, " +
                        "head([rel IN r | type(rel)]) AS relationType, " +
                        "head([rel IN r | rel.type]) AS relationSubType, " +
                        "length(r) AS depth",
                        Values.parameters("id", articleId)
                );

                List<Map<String, Object>> articles = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("articleId", record.get("articleId").asObject());
                    entry.put("relationType", record.get("relationType").asString(""));
                    entry.put("relationSubType", record.get("relationSubType").asString(""));
                    entry.put("depth", record.get("depth").asInt(1));
                    articles.add(entry);
                }
                log.debug("Neo4j findRelatedArticles: id={}, depth={}, found={}", articleId, depth, articles.size());
                return articles;
            });
        } catch (Exception e) {
            log.warn("Neo4j findRelatedArticles failed for id={}: {}", articleId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addClauseRelation(String fromClause, String toClause, String relationType) {
        if (!available) {
            log.warn("Neo4j unavailable, skipping addClauseRelation: {} -> {} ({})",
                    fromClause, toClause, relationType);
            return;
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                        "MERGE (a:Clause {clauseId: $from}) " +
                        "MERGE (b:Clause {clauseId: $to}) " +
                        "MERGE (a)-[r:RELATES_TO {type: $relType}]->(b)",
                        Values.parameters("from", fromClause, "to", toClause, "relType", relationType)
                );
                return null;
            });
            log.debug("Neo4j addClauseRelation: {} -> {} ({})", fromClause, toClause, relationType);
        } catch (Exception e) {
            log.warn("Neo4j addClauseRelation failed: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> findClauseRelations(String clauseId, int depth) {
        if (!available) {
            log.debug("Neo4j unavailable, returning empty results for findClauseRelations: id={}", clauseId);
            return Collections.emptyList();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(
                        "MATCH (a:Clause {clauseId: $id})-[r*1.." + Math.max(1, depth) + "]->(b:Clause) " +
                        "RETURN DISTINCT b.clauseId AS clauseId, " +
                        "head([rel IN r | type(rel)]) AS relationType, " +
                        "head([rel IN r | rel.type]) AS relationSubType, " +
                        "length(r) AS depth",
                        Values.parameters("id", clauseId)
                );

                List<Map<String, Object>> clauses = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("clauseId", record.get("clauseId").asString(""));
                    entry.put("relationType", record.get("relationType").asString(""));
                    entry.put("relationSubType", record.get("relationSubType").asString(""));
                    entry.put("depth", record.get("depth").asInt(1));
                    clauses.add(entry);
                }
                log.debug("Neo4j findClauseRelations: id={}, depth={}, found={}", clauseId, depth, clauses.size());
                return clauses;
            });
        } catch (Exception e) {
            log.warn("Neo4j findClauseRelations failed for id={}: {}", clauseId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
