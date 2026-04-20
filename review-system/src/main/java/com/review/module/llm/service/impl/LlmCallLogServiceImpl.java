package com.review.module.llm.service.impl;

import com.review.module.llm.entity.LlmCallLogDO;
import com.review.module.llm.repository.LlmCallLogRepository;
import com.review.module.llm.service.LlmCallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmCallLogServiceImpl implements LlmCallLogService {

    private final LlmCallLogRepository llmCallLogRepository;

    @Override
    @Transactional
    public LlmCallLogDO log(LlmCallLogDO logEntry) {
        return llmCallLogRepository.save(logEntry);
    }

    @Override
    public List<LlmCallLogDO> listByTaskId(Long taskId) {
        return llmCallLogRepository.findByReviewTaskIdOrderByCreatedAtDesc(taskId);
    }
}
