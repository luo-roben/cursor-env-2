package com.review.infrastructure.vector;

import com.review.infrastructure.vector.dto.VectorSearchResult;

import java.util.List;
import java.util.Map;

public interface VectorStoreService {

    List<VectorSearchResult> search(String text, String collection, int topK);

    void index(String id, String text, String collection, Map<String, Object> metadata);
}
