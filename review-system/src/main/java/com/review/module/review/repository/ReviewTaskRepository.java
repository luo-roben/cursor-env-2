package com.review.module.review.repository;

import com.review.module.review.entity.ReviewTaskDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTaskDO, Long> {

    Optional<ReviewTaskDO> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT rt FROM ReviewTaskDO rt WHERE rt.tenantId = :tenantId "
            + "AND (:reviewStatus IS NULL OR rt.reviewStatus = :reviewStatus) "
            + "AND (:documentType IS NULL OR rt.documentType = :documentType) "
            + "AND (:contentType IS NULL OR rt.contentType = :contentType)")
    Page<ReviewTaskDO> findByTenantIdAndFilters(
            @Param("tenantId") Long tenantId,
            @Param("reviewStatus") String reviewStatus,
            @Param("documentType") String documentType,
            @Param("contentType") String contentType,
            Pageable pageable);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndReviewStatus(Long tenantId, String reviewStatus);

    long countByTenantIdAndOverallVerdict(Long tenantId, String overallVerdict);

    long countByTenantIdAndRiskLevel(Long tenantId, String riskLevel);

    long countByReviewStatus(String reviewStatus);

    long countByOverallVerdict(String overallVerdict);

    long countByRiskLevel(String riskLevel);

    @Query("SELECT AVG(rt.riskScore) FROM ReviewTaskDO rt WHERE rt.tenantId = :tenantId AND rt.reviewStatus = 'COMPLETED'")
    Double avgRiskScoreByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT AVG(rt.riskScore) FROM ReviewTaskDO rt WHERE rt.reviewStatus = 'COMPLETED'")
    Double avgRiskScoreAll();

    @Query("SELECT rt.documentType, COUNT(rt) FROM ReviewTaskDO rt WHERE rt.tenantId = :tenantId GROUP BY rt.documentType")
    List<Object[]> countByDocumentType(@Param("tenantId") Long tenantId);

    @Query("SELECT rt.documentType, COUNT(rt) FROM ReviewTaskDO rt GROUP BY rt.documentType")
    List<Object[]> countByDocumentTypeAll();

    @Query("SELECT rt.reviewStatus, COUNT(rt) FROM ReviewTaskDO rt WHERE rt.tenantId = :tenantId GROUP BY rt.reviewStatus")
    List<Object[]> countByStatus(@Param("tenantId") Long tenantId);

    @Query("SELECT rt.reviewStatus, COUNT(rt) FROM ReviewTaskDO rt GROUP BY rt.reviewStatus")
    List<Object[]> countByStatusAll();
}
