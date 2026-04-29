package com.review.module.cases.repository;

import com.review.module.cases.entity.ReviewCaseDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewCaseRepository extends JpaRepository<ReviewCaseDO, Long> {

    List<ReviewCaseDO> findByTenantIdAndStatus(Long tenantId, String status);

    Page<ReviewCaseDO> findByTenantId(Long tenantId, Pageable pageable);

    List<ReviewCaseDO> findByContentTypeAndStatus(String contentType, String status);

    List<ReviewCaseDO> findByDocumentTypeAndStatus(String documentType, String status);

    List<ReviewCaseDO> findByIsTypicalTrue();

    @Query("SELECT c FROM ReviewCaseDO c WHERE c.status = 'active' AND c.createdAt < :cutoff")
    List<ReviewCaseDO> findActiveOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT c FROM ReviewCaseDO c WHERE c.status = 'active' AND c.createdAt < :cutoff AND (c.lastHitAt IS NULL OR c.lastHitAt < :hitCutoff)")
    List<ReviewCaseDO> findActiveOlderThanWithNoRecentHits(@Param("cutoff") LocalDateTime cutoff, @Param("hitCutoff") LocalDateTime hitCutoff);
}
