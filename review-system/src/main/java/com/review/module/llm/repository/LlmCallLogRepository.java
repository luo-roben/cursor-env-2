package com.review.module.llm.repository;

import com.review.module.llm.entity.LlmCallLogDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmCallLogRepository extends JpaRepository<LlmCallLogDO, Long> {

    List<LlmCallLogDO> findByReviewTaskIdOrderByCreatedAtDesc(Long reviewTaskId);
}
