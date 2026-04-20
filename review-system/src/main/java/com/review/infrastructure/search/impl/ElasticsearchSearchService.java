package com.review.infrastructure.search.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.review.infrastructure.search.FullTextSearchService;
import com.review.infrastructure.search.dto.SearchResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("elasticsearchSearchService")
@ConditionalOnProperty(name = "review.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchSearchService implements FullTextSearchService {

    @Value("${review.elasticsearch.host:localhost}")
    private String host;

    @Value("${review.elasticsearch.port:9200}")
    private int port;

    private RestClient restClient;
    private volatile boolean available = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> initializedIndices = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        try {
            restClient = RestClient.builder(new HttpHost(host, port, "http"))
                    .setRequestConfigCallback(config -> config
                            .setConnectTimeout(5000)
                            .setSocketTimeout(30000))
                    .build();
            available = true;
            log.info("Elasticsearch search service connected to {}:{}", host, port);
        } catch (Exception e) {
            log.warn("Failed to create Elasticsearch client for {}:{} - service will be unavailable: {}",
                    host, port, e.getMessage());
            available = false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (restClient != null) {
            try {
                restClient.close();
            } catch (Exception e) {
                log.warn("Error closing Elasticsearch client: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<SearchResult> search(String query, String index, int topK) {
        if (!available) {
            log.debug("Elasticsearch unavailable, returning empty results for index={}", index);
            return Collections.emptyList();
        }
        try {
            ObjectNode queryBody = objectMapper.createObjectNode();
            queryBody.put("size", topK);

            ObjectNode multiMatch = objectMapper.createObjectNode();
            multiMatch.put("query", query);
            ArrayNode fields = multiMatch.putArray("fields");
            fields.add("content");
            fields.add("title");
            multiMatch.put("type", "best_fields");

            ObjectNode queryNode = objectMapper.createObjectNode();
            queryNode.set("multi_match", multiMatch);
            queryBody.set("query", queryNode);

            Request request = new Request("POST", "/" + index + "/_search");
            request.setJsonEntity(objectMapper.writeValueAsString(queryBody));

            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode responseJson = objectMapper.readTree(responseBody);

            List<SearchResult> results = new ArrayList<>();
            JsonNode hits = responseJson.path("hits").path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    String id = hit.path("_id").asText("");
                    double score = hit.path("_score").asDouble(0.0);
                    JsonNode source = hit.path("_source");

                    String content = source.path("content").asText("");
                    String snippet = content.length() > 500 ? content.substring(0, 500) + "..." : content;

                    Map<String, Object> metadata = new HashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> fieldIterator = source.fields();
                    while (fieldIterator.hasNext()) {
                        Map.Entry<String, JsonNode> field = fieldIterator.next();
                        if (!"content".equals(field.getKey())) {
                            metadata.put(field.getKey(), nodeToObject(field.getValue()));
                        }
                    }

                    results.add(SearchResult.builder()
                            .id(id)
                            .score(score)
                            .content(snippet)
                            .metadata(metadata)
                            .build());
                }
            }

            log.debug("ES search in index={} returned {} results", index, results.size());
            return results;
        } catch (Exception e) {
            log.warn("ES search failed in index={}: {}", index, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void index(String id, String content, String index, Map<String, Object> metadata) {
        if (!available) {
            log.warn("Elasticsearch unavailable, skipping index for id={} in index={}", id, index);
            return;
        }
        try {
            ensureIndexExists(index);

            ObjectNode doc = objectMapper.createObjectNode();
            doc.put("content", content);
            if (metadata != null) {
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    doc.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
                }
            }

            Request request = new Request("PUT", "/" + index + "/_doc/" + id);
            request.setJsonEntity(objectMapper.writeValueAsString(doc));

            restClient.performRequest(request);
            log.debug("ES indexed id={} in index={}", id, index);
        } catch (Exception e) {
            log.warn("ES index failed for id={} in index={}: {}", id, index, e.getMessage());
        }
    }

    private void ensureIndexExists(String index) {
        if (initializedIndices.contains(index)) {
            return;
        }
        try {
            Request headReq = new Request("HEAD", "/" + index);
            Response headResp = restClient.performRequest(headReq);
            if (headResp.getStatusLine().getStatusCode() == 200) {
                initializedIndices.add(index);
                return;
            }
        } catch (Exception e) {
            // index does not exist, create it
        }

        try {
            ObjectNode settings = objectMapper.createObjectNode();
            ObjectNode settingsInner = settings.putObject("settings");
            settingsInner.put("number_of_shards", 1);
            settingsInner.put("number_of_replicas", 0);

            ObjectNode analysis = settingsInner.putObject("analysis");
            ObjectNode analyzers = analysis.putObject("analyzer");
            ObjectNode contentAnalyzer = analyzers.putObject("content_analyzer");
            contentAnalyzer.put("type", "standard");

            ObjectNode mappings = settings.putObject("mappings");
            ObjectNode properties = mappings.putObject("properties");

            ObjectNode contentField = properties.putObject("content");
            contentField.put("type", "text");
            contentField.put("analyzer", "standard");

            ObjectNode titleField = properties.putObject("title");
            titleField.put("type", "text");
            titleField.put("analyzer", "standard");

            Request createReq = new Request("PUT", "/" + index);
            createReq.setJsonEntity(objectMapper.writeValueAsString(settings));

            restClient.performRequest(createReq);
            initializedIndices.add(index);
            log.info("Created Elasticsearch index: {}", index);
        } catch (Exception e) {
            log.warn("Failed to create ES index {}: {}", index, e.getMessage());
            initializedIndices.add(index);
        }
    }

    private Object nodeToObject(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(nodeToObject(item));
            }
            return list;
        }
        if (node.isObject()) {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        }
        return node.asText();
    }
}
