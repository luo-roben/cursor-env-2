package com.compliance.module.cases.repository;

import com.compliance.module.cases.entity.ReviewCaseDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCaseRepository extends JpaRepository<ReviewCaseDO, Long> {

    List<ReviewCaseDO> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<ReviewCaseDO> findByTenantIdAndVerdict(Long tenantId, String verdict);

    @Query("SELECT rc FROM ReviewCaseDO rc WHERE rc.tenantId = :tenantId " +
           "AND (:verdict IS NULL OR rc.verdict = :verdict) " +
           "AND (:contentType IS NULL OR rc.contentType = :contentType) " +
           "AND (:productType IS NULL OR rc.productType = :productType) " +
           "ORDER BY rc.createdAt DESC")
    Page<ReviewCaseDO> findByFilters(
            @Param("tenantId") Long tenantId,
            @Param("verdict") String verdict,
            @Param("contentType") String contentType,
            @Param("productType") String productType,
            Pageable pageable);
}
