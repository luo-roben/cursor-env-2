package com.review.module.review.repository;

import com.review.module.review.entity.ReviewTaskDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
