package com.review.infrastructure.port;

import java.util.List;
import java.util.Optional;

public interface LawArticlePort {

    Optional<Object> findById(Long id);

    List<Object> findByArticleId(String articleId);

    List<Object> findByLawNameContaining(String lawName);

    List<Object> findByApplicableContentType(String contentType);

    List<Object> findByApplicableProductType(String productType);

    List<Object> findAll();
}
