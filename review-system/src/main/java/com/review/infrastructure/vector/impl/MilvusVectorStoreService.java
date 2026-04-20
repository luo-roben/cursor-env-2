package com.review.infrastructure.vector.impl;

import com.alibaba.fastjson.JSONObject;
import com.review.infrastructure.vector.VectorStoreService;
import com.review.infrastructure.vector.dto.VectorSearchResult;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("milvusVectorStoreService")
@ConditionalOnProperty(name = "review.milvus.enabled", havingValue = "true")
public class MilvusVectorStoreService implements VectorStoreService {

    private static final int EMBEDDING_DIM = 128;
    private static final String FIELD_ID = "id";
    private static final String FIELD_EMBEDDING = "embedding";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_METADATA = "metadata";

    @Value("${review.milvus.host:localhost}")
    private String host;

    @Value("${review.milvus.port:19530}")
    private int port;

    @Value("${review.milvus.law-collection:law_knowledge_vectors}")
    private String lawCollection;

    @Value("${review.milvus.case-collection:review_cases_vectors}")
    private String caseCollection;

    @Value("${review.milvus.template-collection:contract_template_vectors}")
    private String templateCollection;

    private MilvusClientV2 client;
    private volatile boolean available = false;
    private final Set<String> initializedCollections = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        try {
            ConnectConfig config = ConnectConfig.builder()
                    .uri("http://" + host + ":" + port)
                    .build();
            client = new MilvusClientV2(config);
            available = true;
            log.info("Milvus vector store service connected to {}:{}", host, port);
        } catch (Exception e) {
            log.warn("Failed to connect to Milvus at {}:{} - service will be unavailable: {}",
                    host, port, e.getMessage());
            available = false;
        }
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            try {
                client.close(3000);
            } catch (Exception e) {
                log.warn("Error closing Milvus client: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<VectorSearchResult> search(String text, String collection, int topK) {
        if (!available) {
            log.debug("Milvus unavailable, returning empty results for search in collection={}", collection);
            return Collections.emptyList();
        }
        try {
            List<Float> embedding = generateEmbedding(text);

            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("metric_type", "COSINE");

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collection)
                    .annsField(FIELD_EMBEDDING)
                    .data(Collections.singletonList(embedding))
                    .topK(topK)
                    .outputFields(Arrays.asList(FIELD_ID, FIELD_TEXT, FIELD_METADATA))
                    .searchParams(searchParams)
                    .build();

            SearchResp searchResp = client.search(searchReq);
            List<List<SearchResp.SearchResult>> results = searchResp.getSearchResults();

            List<VectorSearchResult> output = new ArrayList<>();
            if (results != null && !results.isEmpty()) {
                for (SearchResp.SearchResult hit : results.get(0)) {
                    Map<String, Object> entity = hit.getEntity();
                    Map<String, Object> metadata = new HashMap<>();
                    if (entity != null) {
                        if (entity.containsKey(FIELD_TEXT)) {
                            metadata.put("text", entity.get(FIELD_TEXT));
                        }
                        if (entity.containsKey(FIELD_METADATA)) {
                            Object metaObj = entity.get(FIELD_METADATA);
                            if (metaObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> metaMap = (Map<String, Object>) metaObj;
                                metadata.putAll(metaMap);
                            } else if (metaObj instanceof String metaStr) {
                                try {
                                    JSONObject parsed = JSONObject.parseObject(metaStr);
                                    if (parsed != null) {
                                        metadata.putAll(parsed);
                                    }
                                } catch (Exception ignored) {
                                    metadata.put("metadata", metaStr);
                                }
                            }
                        }
                    }

                    output.add(VectorSearchResult.builder()
                            .id(hit.getId() != null ? hit.getId().toString() : "")
                            .score(hit.getDistance() != null ? hit.getDistance().doubleValue() : 0.0)
                            .metadata(metadata)
                            .build());
                }
            }

            log.debug("Milvus search in collection={} returned {} results", collection, output.size());
            return output;
        } catch (Exception e) {
            log.warn("Milvus search failed in collection={}: {}", collection, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void index(String id, String text, String collection, Map<String, Object> metadata) {
        if (!available) {
            log.warn("Milvus unavailable, skipping index for id={} in collection={}", id, collection);
            return;
        }
        try {
            ensureCollectionExists(collection);

            List<Float> embedding = generateEmbedding(text);

            JSONObject row = new JSONObject();
            row.put(FIELD_ID, id);
            row.put(FIELD_EMBEDDING, embedding);
            row.put(FIELD_TEXT, text.length() > 65535 ? text.substring(0, 65535) : text);
            row.put(FIELD_METADATA, metadata != null ? new JSONObject(metadata).toJSONString() : "{}");

            UpsertReq upsertReq = UpsertReq.builder()
                    .collectionName(collection)
                    .data(Collections.singletonList(row))
                    .build();

            client.upsert(upsertReq);
            log.debug("Milvus indexed id={} in collection={}", id, collection);
        } catch (Exception e) {
            log.warn("Milvus index failed for id={} in collection={}: {}", id, collection, e.getMessage());
        }
    }

    private void ensureCollectionExists(String collection) {
        if (initializedCollections.contains(collection)) {
            return;
        }
        try {
            Boolean exists = client.hasCollection(
                    HasCollectionReq.builder().collectionName(collection).build());
            if (Boolean.TRUE.equals(exists)) {
                initializedCollections.add(collection);
                return;
            }

            CreateCollectionReq.CollectionSchema schema = client.createSchema();
            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_ID)
                    .dataType(DataType.VarChar)
                    .maxLength(256)
                    .isPrimaryKey(true)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_EMBEDDING)
                    .dataType(DataType.FloatVector)
                    .dimension(EMBEDDING_DIM)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_TEXT)
                    .dataType(DataType.VarChar)
                    .maxLength(65535)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_METADATA)
                    .dataType(DataType.VarChar)
                    .maxLength(65535)
                    .build());

            IndexParam indexParam = IndexParam.builder()
                    .fieldName(FIELD_EMBEDDING)
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.COSINE)
                    .build();

            CreateCollectionReq createReq = CreateCollectionReq.builder()
                    .collectionName(collection)
                    .collectionSchema(schema)
                    .indexParams(Collections.singletonList(indexParam))
                    .build();

            client.createCollection(createReq);

            client.loadCollection(LoadCollectionReq.builder()
                    .collectionName(collection)
                    .build());

            initializedCollections.add(collection);
            log.info("Created and loaded Milvus collection: {}", collection);
        } catch (Exception e) {
            log.warn("Failed to ensure collection {} exists: {}", collection, e.getMessage());
        }
    }

    private List<Float> generateEmbedding(String text) {
        float[] embedding = new float[EMBEDDING_DIM];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            Random rng = new Random(bytesToLong(hash));
            float norm = 0f;
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] = (float) rng.nextGaussian();
                norm += embedding[i] * embedding[i];
            }
            norm = (float) Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < EMBEDDING_DIM; i++) {
                    embedding[i] /= norm;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Random fallback = new Random(text.hashCode());
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] = fallback.nextFloat() * 2 - 1;
            }
        }

        List<Float> result = new ArrayList<>(EMBEDDING_DIM);
        for (float v : embedding) {
            result.add(v);
        }
        return result;
    }

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
    }
}
