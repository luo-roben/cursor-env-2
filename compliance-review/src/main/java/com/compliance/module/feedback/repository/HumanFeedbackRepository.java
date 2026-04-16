package com.compliance.module.feedback.repository;

import com.compliance.module.feedback.entity.HumanFeedbackDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HumanFeedbackRepository extends JpaRepository<HumanFeedbackDO, Long> {

    List<HumanFeedbackDO> findByTaskId(Long taskId);

    List<HumanFeedbackDO> findByTaskIdAndTenantId(Long taskId, Long tenantId);

    List<HumanFeedbackDO> findByReviewerId(Long reviewerId);

    long countByTenantIdAndAction(Long tenantId, String action);
}
