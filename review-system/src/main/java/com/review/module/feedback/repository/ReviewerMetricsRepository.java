package com.review.module.feedback.repository;

import com.review.module.feedback.entity.ReviewerMetricsDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewerMetricsRepository extends JpaRepository<ReviewerMetricsDO, Long> {

    Optional<ReviewerMetricsDO> findByReviewerIdAndTenantIdAndPeriod(Long reviewerId, Long tenantId, String period);

    List<ReviewerMetricsDO> findByTenantIdAndPeriod(Long tenantId, String period);

    List<ReviewerMetricsDO> findByReviewerIdAndTenantId(Long reviewerId, Long tenantId);
}
