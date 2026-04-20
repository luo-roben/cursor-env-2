package com.review.module.knowledge.repository;

import com.review.module.knowledge.entity.LawArticleDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawArticleRepository extends JpaRepository<LawArticleDO, Long> {

    Optional<LawArticleDO> findByArticleIdAndLawName(String articleId, String lawName);

    @Query("SELECT la FROM LawArticleDO la WHERE la.status = 'published' AND la.applicableContentTypes LIKE CONCAT('%', :contentType, '%')")
    List<LawArticleDO> findPublishedByContentType(@Param("contentType") String contentType);

    @Query("SELECT la FROM LawArticleDO la WHERE la.status = 'published' AND la.applicableProductTypes LIKE CONCAT('%', :productType, '%')")
    List<LawArticleDO> findPublishedByProductType(@Param("productType") String productType);

    List<LawArticleDO> findByStatus(String status);

    Optional<LawArticleDO> findByArticleIdAndStatus(String articleId, String status);

    @Query("SELECT la FROM LawArticleDO la WHERE "
            + "(:lawName IS NULL OR la.lawName LIKE CONCAT('%', :lawName, '%')) "
            + "AND (:articleId IS NULL OR la.articleId = :articleId) "
            + "AND (:normType IS NULL OR la.normType = :normType) "
            + "AND (:status IS NULL OR la.status = :status) "
            + "AND (:authorityLevel IS NULL OR la.authorityLevel = :authorityLevel)")
    Page<LawArticleDO> findByFilters(
            @Param("lawName") String lawName,
            @Param("articleId") String articleId,
            @Param("normType") String normType,
            @Param("status") String status,
            @Param("authorityLevel") Integer authorityLevel,
            Pageable pageable);
}
