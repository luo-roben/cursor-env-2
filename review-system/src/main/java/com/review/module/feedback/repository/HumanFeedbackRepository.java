package com.review.module.feedback.repository;

import com.review.module.feedback.entity.HumanFeedbackDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HumanFeedbackRepository extends JpaRepository<HumanFeedbackDO, Long> {

    List<HumanFeedbackDO> findByTaskId(Long taskId);

    List<HumanFeedbackDO> findByTenantId(Long tenantId);

    List<HumanFeedbackDO> findByResultId(Long resultId);
}
