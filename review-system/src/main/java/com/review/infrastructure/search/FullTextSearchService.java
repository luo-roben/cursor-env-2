package com.review.infrastructure.search;

import com.review.infrastructure.search.dto.SearchResult;

import java.util.List;
import java.util.Map;

public interface FullTextSearchService {

    List<SearchResult> search(String query, String index, int topK);

    void index(String id, String content, String index, Map<String, Object> metadata);
}
