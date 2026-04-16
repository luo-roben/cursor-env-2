package com.compliance.module.llm.repository;

import com.compliance.module.llm.entity.LlmCallLogDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmCallLogRepository extends JpaRepository<LlmCallLogDO, Long> {

    List<LlmCallLogDO> findByReviewTaskId(Long reviewTaskId);

    List<LlmCallLogDO> findByCallType(String callType);
}
